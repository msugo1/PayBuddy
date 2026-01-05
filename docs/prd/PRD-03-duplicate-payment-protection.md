# PRD: Redis Payment Protection

## 개요

Redis를 활용한 결제 보호 레이어를 구현한다. 동시 결제 요청 차단(Exclusive Gate)과 멱등성(Idempotency)을 하나의 기능으로 통합한다.

### 목표
- 동시 결제 요청 시 하나만 통과, 나머지는 즉시 거부 (fail-fast)
- Idempotency Key 기반 멱등성 보장
- 기존 in-memory 구현을 분산 환경 지원으로 교체

### 기술 스택
- Redis 7 (LTS, 2028년까지 지원)
- Spring Data Redis + Lettuce
- Docker Compose (로컬 개발)

### 브랜치
- `feature/redis-payment-protection`

---

## 핵심 결정 사항

### Redis Key Patterns
- Exclusive Gate: `payment:gate:{merchantId}:{orderId}` (TTL: 30s)
- Idempotency: `payment:idempotency:{idempotencyKey}` (TTL: 24h)

### Response Status Codes
| Scenario | Status | error_code |
|----------|--------|------------|
| 첫 번째 성공 요청 | 201 Created | - |
| 캐시된 응답 (재시도) | 200 OK | - |
| 동시 요청 차단 | 409 Conflict | DUPLICATE_PAYMENT_REQUEST |
| Idempotency 충돌 | 409 Conflict | PAYMENT_REQUEST_MISMATCH |

### Gate Safety
- UUID 기반 owner로 다른 인스턴스의 exit 호출 방지
- finally 블록에서 반드시 exit

---

## Phase 1: Exclusive Gate (동시 요청 차단)

### 목표
`PaymentSessionService.prepare()`의 race condition 해결
- 하나만 통과, 나머지는 즉시 거부

### Task 1: Redis 의존성 추가
- `gradle/libs.versions.toml`에 `spring-boot-starter-data-redis` 추가
- `payment/build.gradle.kts`에 Redis 의존성 추가

### Task 2: Docker Compose Redis 설정
- `docker-compose.yml`에 Redis 7-alpine 컨테이너 추가
- 환경 변수: REDIS_HOST, REDIS_PORT
- Health check 설정

### Task 3: Redis 연결 설정
- `application.yml`에 Redis 데이터소스 설정
- `payment.gate.timeout-seconds` 프로퍼티 추가 (기본값: 30)

### Task 4: ExclusivePaymentGate 인터페이스 정의
- `domain/ExclusivePaymentGate.kt` 생성
- `tryEnter(merchantId: String, orderId: String): Boolean`
- `exit(merchantId: String, orderId: String)`

### Task 5: PaymentGateRejectedException 정의
- `domain/PaymentGateRejectedException.kt` 생성
- merchantId, orderId 프로퍼티 포함

### Task 6: Redis 설정 클래스 구현
- `infrastructure/redis/PaymentGateProperties.kt` 생성 (@ConfigurationProperties)
- `infrastructure/redis/RedisConfiguration.kt` 생성

### Task 7: RedisExclusivePaymentGate 구현
- `infrastructure/redis/RedisExclusivePaymentGate.kt` 생성
- Redis SETNX (setIfAbsent) 사용
- UUID 기반 lock owner 패턴

### Task 8: PaymentSessionService에 Gate 통합
- `ExclusivePaymentGate` 의존성 주입
- `prepare()` 메서드에 try-finally 패턴 적용
- Gate 진입 실패 시 `PaymentGateRejectedException` throw

### Task 9: Gate 예외 핸들러 추가
- `PaymentsApiController`에 `@ExceptionHandler` 추가
- 409 Conflict 응답 (error_code: DUPLICATE_PAYMENT_REQUEST)

### Task 10: Gate 단위 테스트 작성
- `FakeExclusivePaymentGate` 구현
- `PaymentSessionServiceTest` 확장
  - Gate 진입 실패 시 예외 발생 테스트
  - Gate가 finally에서 해제되는지 테스트

### Task 11: Gate 통합 테스트 작성
- Docker Redis 기반 통합 테스트
- 동시성 테스트 (CountDownLatch 사용)
- TTL 만료 테스트

---

## Phase 2: Idempotency (멱등성)

### 목표
`PaymentsApiController`의 in-memory Map을 Redis로 교체

### Task 12: IdempotencyStore 인터페이스 정의
- `domain/IdempotencyStore.kt` 생성
- `findByKey(idempotencyKey: String): CachedPaymentResponse?`
- `save(idempotencyKey: String, requestHash: String, response: CachedPaymentResponse)`
- `getRequestHash(idempotencyKey: String): String?`

### Task 13: CachedPaymentResponse DTO 정의
- `domain/CachedPaymentResponse.kt` 생성
- paymentKey, checkoutUrl, expiresAt 프로퍼티

### Task 14: Idempotency 설정 프로퍼티 추가
- `infrastructure/redis/IdempotencyProperties.kt` 생성
- `payment.idempotency.ttl-hours` (기본값: 24)
- `RedisConfiguration`에 프로퍼티 등록

### Task 15: RedisIdempotencyStore 구현
- `infrastructure/redis/RedisIdempotencyStore.kt` 생성
- Redis Hash 구조 사용 (requestHash, response)
- Jackson ObjectMapper로 JSON 직렬화

### Task 16: IdempotencyService 구현
- `IdempotencyService.kt` 생성
- `checkAndGetCached()`: 캐시 조회 및 해시 비교
- `cacheResponse()`: 응답 캐싱
- SHA-256 기반 요청 해시 계산

### Task 17: PaymentsApiController에 Idempotency 통합
- `IdempotencyService` 의존성 주입
- in-memory `idempotencyStorage` 제거
- `readyPayment()` 메서드 수정:
  - 캐시 조회 → 있으면 200 OK 반환
  - 없으면 처리 → 캐싱 → 201 Created 반환

### Task 18: Idempotency 예외 핸들러 수정
- 기존 `IdempotencyConflictException` 핸들러 유지
- error_code를 `PAYMENT_REQUEST_MISMATCH`로 변경

### Task 19: Idempotency 단위 테스트 작성
- `FakeIdempotencyStore` 구현
- `IdempotencyServiceTest` 작성
  - 첫 요청 시 null 반환
  - 동일 요청 재시도 시 캐시 반환
  - 다른 요청으로 같은 키 사용 시 예외

### Task 20: Idempotency 통합 테스트 작성
- Docker Redis 기반 통합 테스트
- TTL 만료 테스트
- 동시 접근 테스트

---

## 성공 기준

### Phase 1: Exclusive Gate
- [ ] Docker Redis로 로컬 개발 환경 실행 가능
- [ ] 동시 결제 요청 시 하나만 통과, 나머지는 409 Conflict
- [ ] Gate가 정상/예외 상황 모두에서 해제됨
- [ ] 기존 단위 테스트 통과

### Phase 2: Idempotency
- [ ] 같은 Idempotency Key로 동일 요청 시 캐시된 응답 반환
- [ ] 같은 Key로 다른 요청 시 409 Conflict (PAYMENT_REQUEST_MISMATCH)
- [ ] in-memory 구현 완전 제거
- [ ] 24시간 후 캐시 만료
