# PRD: Payment Submit 유즈케이스

## 개요

결제창에서 사용자가 결제수단을 선택하고 제출(submit)하는 과정을 구현한다.

**목표**: 카드 결제 submit 처리, 가맹점/결제수단 검증, FDS 체크, 인증 필요 여부 판단, 프로모션 적용

**범위**: 1차 카드 결제만 구현. 가상계좌/간편결제는 확장 포인트만 남겨두고 향후 추가.

**브랜치**: `feat/submit-payment-method`

---

## 기술적 의사결정

| # | 주제 | 대안 | 선택 | 이유 |
|---|------|------|------|------|
| 1 | Submit/Confirm 분리 | A: 합쳐서 승인까지 / B: 분리 | B | 유저 이탈/장애 시 승인 전이라 후처리 불필요. 기술적 필연성은 없으나 edge case 대응 단순화 |
| 2 | 검증 실패 시 Payment | A: 검증 후 생성 / B: 먼저 생성→실패 시 FAILED | B | 실패 사유 기록으로 디버깅/CS 대응 용이 |
| 3 | PaymentDetails 구조 | A: Method+Result 분리 / B: 통합 | B | 결제수단별 result 구조 다름. sealed class로 확장 자연스러움 |
| 4 | PaymentResult 타입 | A: Generic / B: 공통 타입 | B | Generic은 매핑/직렬화 복잡. 현재 요구사항에 오버엔지니어링 |
| 5 | finalAmount 저장 | A: DB 저장 / B: 계산 프로퍼티 | B | EffectivePromotion이 스냅샷. 중복 저장 시 불일치 위험 |
| 6 | Payment 내 필드명 | A: sessionId / B: paymentKey | B | API에서 payment_key로 노출. 외부 키 관점이 자연스러움 |
| 7 | 동시결제 방지 (Submit) | Redis gate + 상태체크 | - | 동일 세션 동시 요청 차단 |

---

## 상태 전이

```kotlin
enum class PaymentStatus {
    INITIALIZED,              // 생성됨
    AUTHENTICATION_REQUIRED,  // 인증 대기
    PENDING_CONFIRM,          // Confirm 대기
    PAYMENT_PROCESSING,       // 카드사 승인 요청 중
    COMPLETED,                // 완료
    FAILED,                   // 실패
    CANCELLED,                // 취소됨
}
```

```
INITIALIZED → 검증 실행
    ├── FAILED (검증 실패)
    ├── AUTHENTICATION_REQUIRED (인증 필요) → 인증 성공 → PENDING_CONFIRM
    └── PENDING_CONFIRM (인증 불필요)
```

**Submit 결과**: `AUTHENTICATION_REQUIRED`, `PENDING_CONFIRM`, `FAILED` 중 하나

---

## Submit 플로우

```
1. PaymentSession 조회 + 만료 체크
2. Gate 진입 (sessionId 기준)
3. 기존 Payment 확인 (PAYMENT_PROCESSING → 에러, 그 외 → CANCELLED)
4. Payment 생성 (INITIALIZED)
5. payment.submit(cardDetails) - 카드 정보 먼저 설정 (검증 실패 시 fail() 안전성)
6. 검증 (가맹점, 한도, BIN, FDS, 할부) → 실패 시 payment.fail()
7. 프로모션 적용 (payment.addPromotion(promotion, minPaymentAmount))
8. 카드 토큰화
9. 인증 필요 여부 판단
10. 상태 전이 (requestAuthentication() / completeWithoutAuthentication()) + Gate 해제 + 응답
```

---

## 엔티티

### Payment
```kotlin
@Entity
class Payment(
    val id: String,                         // ULID
    val paymentKey: String,                 // ULID (= PaymentSession.id)
    val merchantId: String,
    status: PaymentStatus,
    val version: Long,                      // Optimistic Lock
    val originalAmount: Long,
    private val effectivePromotions: MutableList<EffectivePromotion>,
    var cardPaymentDetails: CardPaymentDetails?,  // submit() 후 설정, 검증 전 실패 시에도 존재
    val createdAt: OffsetDateTime,
    var updatedAt: OffsetDateTime,
) {
    var status: PaymentStatus = status
        private set

    val finalAmount: Long
        get() = originalAmount - effectivePromotions.sumOf { it.amount }

    fun submit(cardDetails: CardPaymentDetails)
    fun addPromotion(promotion: EffectivePromotion, minPaymentAmount: Long)
    fun fail(errorCode: String, failureReason: String)
    fun requestAuthentication()
    fun completeAuthentication()
    fun completeWithoutAuthentication()
}
```

### CardPaymentDetails / PaymentResult
```kotlin
@Embeddable
data class CardPaymentDetails(
    @Embedded val card: Card,
    val installmentMonths: Int,
    @Embedded val result: PaymentResult?,
)

@Embeddable
data class PaymentResult(
    val approvalNumber: String?,        // 성공 시
    val approvedAt: OffsetDateTime?,    // 성공 시
    val errorCode: String?,             // 실패 시
    val failureReason: String?,         // 실패 시
)
```

**참고**: sealed class 대신 단순 data class 사용. 복잡도 줄이고 JPA 매핑 단순화.

### Card / EffectivePromotion
```kotlin
@Embeddable
data class Card(
    val maskedNumber: String,           // 1234-56**-****-7890
    val bin: String,                    // BIN (Bank Identification Number)
    val brand: CardBrand?,
    val issuerCode: String,             // 발급사 코드 (04 = 삼성, 06 = 신한 등)
    val acquirerCode: String,           // 매입사 코드
    val cardType: CardType,             // CREDIT, DEBIT, PREPAID
    val ownerType: OwnerType,           // PERSONAL, CORPORATE
    val issuedCountry: String,          // KR, US 등
    val productCode: String?,           // 카드 상품 코드
)

data class EffectivePromotion(
    val name: String,                   // 스냅샷: 프로모션명 (수정되어도 기록 유지)
    val provider: PromotionProvider,    // PLATFORM, CARD_ISSUER, ACQUIRER
    val amount: Long,
)
```

**참고**:
- `EffectivePromotion`은 스냅샷이므로 promotionId 제거 (통계는 별도 이벤트 로그)
- `Card`는 BIN lookup 결과를 포함한 전체 정보 (기존 Card 엔티티 재사용)

---

## 외부 서비스 (1차 stub)

```kotlin
data class MerchantContract(
    val merchantId: String, val status: MerchantStatus, val contractEndDate: LocalDate?,
    val mcc: String,
    val paymentMethodPolicies: Map<PaymentMethodType, PaymentMethodPolicy>,
    val installmentPolicy: MerchantInstallmentPolicy?,
)

interface MerchantLimitService {
    fun check(merchantId: String, paymentMethod: PaymentMethodType, amount: Long): Boolean
    fun consume(merchantId: String, paymentId: String, amount: Long)
    fun restore(merchantId: String, paymentId: String, amount: Long)
}

interface PaymentPolicy {
    val defaultExpireMinutes: Long
    val minPaymentAmount: Long  // 전역 최소 결제 금액 (1차: 1000원)
}
```

**참고**: 최소 결제 금액은 1차 구현에서 전역 정책 사용. 가맹점별 차등 정책은 향후 확장.

---

## API 스펙

### Ready 요청 (buyer 추가)
```yaml
buyer: { name, email, phone }  # optional, 3DS2용
```

### POST /checkout/submit
```yaml
Request:
  payment_key: string
  payment_method: { type: "CARD", card: { card_number, expiry_month, expiry_year, cvc, holder_name, installment_months } }
  browser_info: { screen_width, screen_height, color_depth, time_zone_offset, language }  # optional, 3DS2용

Response:
  payment_key: string
  status: "AUTHENTICATION_REQUIRED" | "PENDING_CONFIRM"
  authentication?: { type, method, url, params/data }  # 인증 필요 시
  redirectUrl?: string                                  # 인증 불필요 시
```

---

## Tasks

### Phase 0-1: 기반
- [ ] Ready API에 buyer 필드 추가 (3DS2용, optional)
- [ ] Payment 엔티티 + JPA + 테스트 (상태 전이, finalAmount 계산)

### Phase 3-5: 검증 로직
- [ ] 가맹점 검증 + 테스트 (계약만료, 결제수단 미허용, 최소금액)
- [ ] BIN 조회 → Card 생성 + 테스트 (전체 필드 매핑)
- [ ] CardVaultService (Redis 임시 저장)
- [ ] FDS (국가차단, Velocity) + 테스트
- [ ] 할부 검증 + 테스트 (무이자 조건, 카드사별 정책)
- [ ] 프로모션 적용 + 테스트 (조건, 중복적용, EffectivePromotion)

### Phase 6: 인증 판단
- [ ] AuthenticationRule + 테스트 (국내/해외, 면제조건)
- [ ] Submit 응답 완성

### Phase 2: Submit UseCase
- [ ] SubmitPaymentUseCase + 테스트 (만료, PROCESSING 에러, CANCELLED 전환)
- [ ] Submit API 엔드포인트

### Phase 8: 통합 테스트
- [ ] Submit E2E, 동시 요청, FDS 테스트

---

## PR 구조

| PR | 내용 | 브랜치 |
|----|------|--------|
| 1 | PRD 업로드 | `docs/prd-payment-submit` |
| 2 | 사전 정리 + 엔티티 | `feat/payment-entities` |
| 3 | 검증 로직 | `feat/payment-validation` |
| 4 | 인증 판단 | `feat/payment-authentication` |
| 5 | Submit UseCase + API | `feat/submit-payment-method` |
| 6 | 통합 테스트 | `test/submit-payment-e2e` |

---

## 성공 기준

- [ ] Submit API로 Payment 생성
- [ ] Payment 상태 전이 정상 동작
- [ ] 동시 Submit 방지 (gate)
- [ ] 가맹점/FDS 검증 동작
- [ ] 인증 필요 여부 판단 + 응답
- [ ] 테스트 통과

---

## 부록: Confirm 구현 시 참고사항

> 이 PRD 범위 밖. 다음 작업 시 참고용.

**confirm** submit API 스펙 변경에 따른 confirm API 스펙변경 반영필요

**플로우**
```
1. Payment 조회 (PENDING_CONFIRM만)
2. 상태 전이 → PAYMENT_PROCESSING (Optimistic Lock, 카드사 호출 전!)
3. 카드사 승인 요청
4. 결과 반영 (COMPLETED / FAILED) + 한도 consume
```

**동시 Confirm 문제**: 동일 Payment에 Confirm 동시 요청 시, 둘 다 카드사 승인 호출하면 이중 결제 발생 가능
