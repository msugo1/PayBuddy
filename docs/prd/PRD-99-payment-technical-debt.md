# PRD: Payment Module Technical Debt

## 개요
Payment 모듈 기술부채 해결 및 성능 최적화

**목표:**
- Peak 20K TPS 대비 DB 병목 제거
- JPA 모범 사례 적용

---

## Task 1: PaymentSession Persistable 구현

### 문제
- ULID 직접 할당 시 `save()` 호출마다 불필요한 SELECT 발생
- Peak 20K TPS 기준 초당 20,000회 불필요한 쿼리

### 해결
```kotlin
@Entity
class PaymentSession(...) : Persistable<String> {
    @Transient
    private var isNew: Boolean = true

    override fun getId(): String = id
    override fun isNew(): Boolean = isNew

    @PostPersist
    @PostLoad
    fun markNotNew() { this.isNew = false }
}
```

### 검증
- 기존 테스트: `JpaPaymentSessionRepositoryTest.새 세션 저장 시 SELECT 쿼리가 먼저 실행되는지 확인`
- SQL 로그: INSERT만 실행, SELECT 제거 확인

**우선순위: High**

---

## Task 2: INSERT ON CONFLICT (UPSERT) 구현

### 문제
- PRD 요구사항 미구현 (`PRD-payment-session-persistence.md:33`)
- 현재 "조회 후 분기" 방식 → 동시 요청 시 Race Condition
- UNIQUE 위반 시 `DataIntegrityViolationException`

### 해결 (권장: Native Query)
```kotlin
@Query(value = """
    INSERT INTO payment_session (...)
    VALUES (...)
    ON CONFLICT (merchant_id, order_id) WHERE (expired = false)
    DO UPDATE SET updated_at = EXCLUDED.updated_at
    RETURNING *
""", nativeQuery = true)
fun upsert(@Param("session") session: PaymentSession): PaymentSession
```

**성능:**
- 기존: 2 queries/request
- 개선: 1 query/request
- **DB 부하 50% 감소**

### 대안: Try-Catch 패턴
```kotlin
try {
    val ongoing = repository.findOngoingPaymentSession(...)
    if (ongoing == null) return repository.save(...)
    return ongoing
} catch (e: DataIntegrityViolationException) {
    repository.findOngoingPaymentSession(...) ?: throw ...
}
```

**트레이드오프:**
- Native Query: PostgreSQL 종속, 성능 우수
- Try-Catch: DB 독립, Exception 제어 흐름 사용

**권장: Native Query** (이미 PostgreSQL 종속적 + 고TPS 목표)

### 검증
- 기존 테스트 통과
- 동시성 테스트 추가 (10개 스레드 동시 요청)

**우선순위: High**

---

## Task 3: Redis 기반 Idempotency 구현

### 문제
- 현재: `PaymentsApiController`의 in-memory Map 사용 (line 54)
- 서버 재시작 시 멱등성 정보 소실
- 멀티 인스턴스 환경에서 동작하지 않음

### 해결
**Domain Layer:**
- `IdempotencyStore` 인터페이스
- `CachedPaymentResponse` DTO (선택)

**Infrastructure Layer:**
- `IdempotencyProperties` (TTL 24시간 설정)
- `RedisIdempotencyStore` 구현

**Service Layer:**
- `IdempotencyService` (캐시 조회/저장, 요청 해시 비교)

**Integration:**
- `PaymentsApiController` 수정
  - `idempotencyStorage` Map 제거
  - `IdempotencyService` 주입
  - 캐시된 응답 있으면 200 OK 반환 (현재: 201 Created)

### Redis Key Pattern
```
payment:idempotency:{idempotencyKey}  # TTL: 24h
  - requestHash: String
  - response: JSON (PaymentReadyResponse)
```

### 검증
- `RedisIdempotencyStoreTest` (통합 테스트)
- `IdempotencyServiceTest` (단위 테스트)
- 동일 요청 재시도 시 캐시된 응답 반환 확인

**우선순위: Medium** (기본 결제 기능 구현 후)

---

## Task 4: Redis 장애 시 ExclusivePaymentGate 우회 방안

### 문제
- 현재: Redis 장애 시 `RedisConnectionFailureException` 발생 → 결제 완전 차단
- `setIfAbsent()` 실패 시 예외가 PaymentSessionService까지 전파

### 해결 방안

**옵션 1: 예외 타입 구체화 (추천)**
```kotlin
override fun tryEnter(...): Boolean {
    return try {
        redisTemplate.opsForValue().setIfAbsent(...) ?: false
    } catch (e: RedisConnectionFailureException) {
        logger.warn("Redis unavailable, bypassing gate", e)
        true  // 장애 시 Gate 우회
    }
}
```

**옵션 2: 설정 기반 Fallback**
```kotlin
@ConfigurationProperties(prefix = "payment.gate")
data class PaymentGateProperties(
    val lockTtlSeconds: Long = 5,
    val bypassOnError: Boolean = true
)
```

**Trade-off:**
- ✅ Redis 정상: 중복 차단 동작
- ✅ Redis 장애: Gate 우회, 결제는 계속 가능
- ❌ Redis 장애 시: DB race condition 가능 (UNIQUE 제약조건으로 최종 방어)

### 검증
- Redis 컨테이너 중지 후 결제 요청 테스트
- 로그에 경고 메시지 출력 확인
- 모니터링 알람 설정 (Redis bypass 발생 시)

**우선순위: Low** (현재는 로컬 개발 환경만 존재, 운영 배포 전 추가)

---

## 성공 기준
- [ ] Persistable 구현으로 SELECT 제거 (SQL 로그 확인)
- [ ] UPSERT 구현으로 동시성 문제 해결
- [ ] Redis 기반 Idempotency 구현 (Phase 2)
- [ ] Redis 장애 시 Gate 우회 (운영 배포 전)
- [ ] 모든 테스트 통과
- [ ] DB 쿼리 수 50% 감소
