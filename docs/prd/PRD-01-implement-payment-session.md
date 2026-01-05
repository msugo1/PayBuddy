# Payment Domain PRD

## 목표
결제 세션 관리(/ready), 결제 실행(/confirm), 다양한 결제 수단 지원

## 브랜치 로드맵
1. `feat/payment-single` - PaymentSession, Payment, VO, /ready API
2. `feat/payment-confirm` - /confirm 승인 로직, CardPaymentProcessor
3. `feat/issuer-policy` - 카드사 정책 검증
4. `feat/merchant` - 가맹점 엔티티
5. `feat/payment-virtual-account` - 가상계좌 추가
6. `feat/card-payment-events` - 도메인 이벤트

---

## 도메인 모델

### Aggregate 설계 이유
- **생명주기 분리**: Session은 /ready, Payment는 /confirm에서 생성
- **1:N 관계**: 하나의 세션에 여러 결제 시도(재시도) 가능

### 구조
```
PaymentSession (AR)                 Payment (AR)
├── id: Long                        ├── id: Long
├── paymentKey: String              ├── paymentSessionId: Long
├── merchantId: String              ├── status: PaymentStatus (수단별)
├── orderId: String                 ├── method: PaymentMethod
├── orderLine: OrderLine            ├── approvalNumber: String?
├── amount: PaymentAmount           ├── approvedAt: Instant?
├── expireAt: Instant               ├── failure: PaymentFailure?
├── successUrl: String              └── createdAt: Instant
├── failUrl: String
└── createdAt: Instant
```

> Session 만료: expireAt 시간 기반 판단 (별도 status 없음)

### 결제수단별 상태

**설계 결정**: Payment는 `IN_PROGRESS`로 시작 (PG 요청 추적, 중복 방지)

```kotlin
sealed interface PaymentStatus

enum class CardPaymentStatus : PaymentStatus {
    IN_PROGRESS,  // PG 승인 요청 중
    AUTHORIZED,   // 가승인 완료 (한도 홀드, 돈 미이동)
    CAPTURED,     // 매입 완료 (실제 이체)
    FAILED,       // 실패/타임아웃
    VOIDED,       // 가승인 취소
    REFUNDED      // 환불
}

enum class VirtualAccountStatus : PaymentStatus {
    IN_PROGRESS,  // PG 발급 요청 중
    PENDING,      // 발급 완료, 입금 대기
    DEPOSITED,    // 입금 완료
    EXPIRED,      // 만료
    REFUNDED      // 환불
}
```

**용어 정리**:
- VOIDED: Authorization 취소 (돈 안 움직임)
- REFUNDED: Capture 후 환불 (돈 되돌림)

JPA 저장: `@Converter`로 `"CARD:CAPTURED"` 형식 직렬화

### Value Objects
| VO | 필드 |
|----|------|
| OrderLine | items: List<OrderLineItem> |
| OrderLineItem | name, quantity, unitAmount, imageUrl |
| PaymentAmount | total, supply, vat |
| PaymentMethod | sealed interface → Card, VirtualAccount |
| PaymentFailure | methodType, code, reason, pgRawCode |

**PaymentFailure 저장**: 도메인은 VO, DB는 별도 테이블 (1:1). 최종 실패만 저장.

### Card 구조
```
Card (PaymentMethod)
├── maskedNumber, bin, brand: CardBrand?
├── issuerCode, acquirerCode  // 한국 카드사 코드
├── cardType: CREDIT|DEBIT|PREPAID
├── ownerType: PERSONAL|CORPORATE
├── issuedCountry, productCode?
```
> CardBrand: BIN에서 파생 가능하나 정책 검증용으로 선택적 저장

### Enums
- CardBrand: VISA, MASTERCARD, AMEX, JCB, UNIONPAY, BC, LOCAL
- CardType: CREDIT, DEBIT, PREPAID
- OwnerType: PERSONAL, CORPORATE

---

## API

> 상세: [docs/openapi/api/payment.yaml](../openapi/api/payment.yaml)

### 스펙 변경 필요
| 필드 | 변경 |
|------|------|
| `redirect_url` | `success_url`로 rename |
| `fail_url` | 추가 필요 |

---

## 비즈니스 규칙

### 금액
- 최소 1,000원, supply + vat = total, 음수 불가

### 세션 만료
- 내부 정책 (외부 입력 불가), 기본 15분 (5~60분)

### 멱등성
| 레이어 | 메커니즘 |
|--------|----------|
| API | `Idempotency-Key` 헤더 캐싱 |
| 도메인 | `merchantId + orderId` 유니크 |

### 상태 전이
```
CardPaymentStatus:
IN_PROGRESS → AUTHORIZED → CAPTURED → REFUNDED
         ├→ FAILED    └→ VOIDED
         └→ CAPTURED (즉시 매입)

VirtualAccountStatus:
IN_PROGRESS → PENDING → DEPOSITED → REFUNDED
         ├→ FAILED    └→ EXPIRED
```

---

## 정책
```kotlin
object PaymentPolicy {
    const val DEFAULT_EXPIRE_MINUTES = 15L
    const val MIN_EXPIRE_MINUTES = 5L
    const val MAX_EXPIRE_MINUTES = 60L
    const val MIN_PAYMENT_AMOUNT = 1000L
}
```
> 향후: 설정 외부화 → 동적 정책 → Policy Aggregate 승격

---

## 테스트 전략
| 기법 | 이유 |
|------|------|
| EP-BVA | 금액/수량 경계 조건 |
| 상태 전이 | 상태 머신 검증 |
| CORRECT | 불변식, 시간 조건 |

---

## 파일 구조
```
payment/src/main/kotlin/com/paybuddy/payment/
├── session/     # PaymentSession, Repository, KeyGenerator
├── payment/     # Payment, Status, Method, Failure
├── card/        # CardType, OwnerType
├── order/       # OrderLine, OrderLineItem
├── amount/      # PaymentAmount
├── policy/      # PaymentPolicy
└── api/         # Controller
```

---

## 향후 고려
| 항목 | 도입 조건 |
|------|----------|
| Outbox 패턴 | 외부 시스템 연동 시 |
| 이벤트 소싱 | 감사/분쟁 요건 시 |
| PaymentFailure 1:N | 재시도 분석 필요 시 |
| Saga 패턴 | 복합 결제 확장 시 |

---

## 참고
- API 스펙: [docs/openapi/api/payment.yaml](../openapi/api/payment.yaml)
- 스키마: [docs/openapi/schemas/payment.yaml](../openapi/schemas/payment.yaml)
