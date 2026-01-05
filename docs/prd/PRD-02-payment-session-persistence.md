# PRD: PaymentSession PostgreSQL Persistence

## 개요

PaymentSession 도메인 모델을 PostgreSQL에 연결하여 실제 데이터베이스 기반 결제 세션 관리를 구현한다.

### 목표
- PostgreSQL 단독으로 동시성 제어 (Redis 없이)
- 확장 가능한 ID 전략 (ULID)

### 기술 스택
- PostgreSQL
- Flyway (마이그레이션)
- Spring Data JPA
- Docker (로컬 개발 환경)

---

## 핵심 결정 사항

### ID 전략
- ULID 사용 (시퀀스 대신)
- 멀티 인스턴스 환경에서 병목 없음
- id = 외부 paymentKey로 노출

### 상태 관리
- PaymentSession에 별도 상태 컬럼 없음
- `expired` 필드로 만료 여부 관리
- 상태 머신은 향후 Payment 엔티티에서 담당

### 동시성 제어
- INSERT ON CONFLICT (UPSERT) 사용
- `UNIQUE INDEX (merchant_id, order_id) WHERE expired = false`
- 같은 주문에 대한 중복 세션 방지

### Value Objects 저장
- PaymentAmount: @Embeddable
- RedirectUrl: @Embeddable
- OrderLine/OrderLineItem: JSONB

---

## 작업 항목

### Task 1: Docker PostgreSQL 개발 환경 설정
- docker-compose.yml 생성
- PostgreSQL 컨테이너 설정
- .env.example 작성

### Task 2: Gradle 의존성 추가
- Spring Data JPA
- PostgreSQL Driver
- Flyway
- ULID 라이브러리 (com.github.f4b6a3:ulid-creator)

### Task 3: application.yml 데이터소스 설정
- 데이터소스 연결 정보
- HikariCP 커넥션 풀 설정
- JPA/Hibernate 설정
- Flyway 설정

### Task 4: ULID 기반 PaymentKeyGenerator 구현
- UlidPaymentKeyGenerator 클래스 생성
- 기존 PaymentKeyGenerator 인터페이스 구현
- 단위 테스트 작성

### Task 5: PaymentSession JPA 엔티티 변환
- PaymentSession @Entity 어노테이션 추가
- PaymentAmount @Embeddable 변환
- RedirectUrl @Embeddable 변환
- OrderLine JSONB AttributeConverter 구현
- updated_at 필드 추가

### Task 6: Flyway 마이그레이션 스크립트 작성
- V1__create_payment_session.sql
- 테이블 스키마:
  - id VARCHAR(26) PRIMARY KEY
  - merchant_id, order_id
  - order_line JSONB
  - total_amount, supply_amount, vat_amount
  - success_url, fail_url
  - expires_at, expired
  - created_at, updated_at
- UNIQUE INDEX: idx_ongoing_payment_session (merchant_id, order_id) WHERE expired = false

### Task 7: JpaPaymentSessionRepository 구현
- Spring Data JPA Repository 인터페이스
- findByMerchantIdAndOrderIdAndExpiredFalse 메서드
- UPSERT 네이티브 쿼리 구현 (INSERT ON CONFLICT)

### Task 8: PaymentSessionRepository 어댑터 구현
- 도메인 Repository 인터페이스 구현체
- JpaPaymentSessionRepository 위임

### Task 9: 통합 테스트 작성
- Docker PostgreSQL 기반 테스트 환경
- application-test.yml 프로파일
- JpaPaymentSessionRepository 통합 테스트
- UPSERT 동시성 테스트

---

## 데이터베이스 스키마

```sql
CREATE TABLE payment_session (
    id VARCHAR(26) PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    order_id VARCHAR(255) NOT NULL,
    order_line JSONB NOT NULL,
    total_amount BIGINT NOT NULL,
    supply_amount BIGINT NOT NULL,
    vat_amount BIGINT NOT NULL,
    success_url VARCHAR(2048) NOT NULL,
    fail_url VARCHAR(2048) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_ongoing_payment_session
ON payment_session (merchant_id, order_id)
WHERE expired = FALSE;
```

---

## 제외 사항 (다음 스코프)

- Idempotency 레이어 (별도 브랜치: feature/idempotency)
- Payment, PaymentAttempt 엔티티 (별도 브랜치: feature/payment-entity)
- 복합 결제 지원
- Redis 캐싱

---

## 성공 기준

- [ ] Docker PostgreSQL로 로컬 개발 환경 실행 가능
- [ ] PaymentSession CRUD 동작
- [ ] 동시 요청 시 중복 세션 생성 방지 (UPSERT)
- [ ] 기존 단위 테스트 통과 (FakeRepository 유지)
- [ ] 통합 테스트 통과
