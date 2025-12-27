# PayBuddy Frontend - Product Requirements Document

## 개요

자체 PG 서버의 결제 플로우를 학습하고, 3DS 인증을 포함한 전체 결제 흐름을 시각적으로 확인하기 위한 프론트엔드 프로젝트입니다.

### 목적
- 3DS 인증 플로우 이해
- 백엔드 API 인터랙션 확인
- Widget (iframe) + Redirect (successUrl) 패턴 학습

### 구현 방식
**Widget (iframe) + Redirect (successUrl)**

카드 입력은 iframe으로 격리, 3DS 인증 및 최종 승인은 전체 페이지 리다이렉트 사용

---

## API 스펙 관리

**⚠️ `/docs/openapi/`가 Source of Truth**

- 백엔드: 스펙 먼저 수정 → 구현
- 프론트엔드: OpenAPI 기반 타입 정의
- `lib/api/types.ts`는 OpenAPI 스펙과 100% 일치

---

## 프로젝트 구조

```
frontend/
├── merchant-demo/          (port 3000, 가맹점)
│   ├── app/
│   │   ├── merchant/       # 상품 목록/상세
│   │   └── success/        # successUrl 처리
│   ├── components/
│   │   ├── ui/             # shadcn/ui
│   │   └── merchant/       # 도메인
│   └── lib/
│       ├── api/            # client, types, payments
│       ├── mock-products.ts
│       └── mocks/          # MSW
│
└── payment-widget/         (port 3001, PG Widget)
    ├── app/
    │   └── widget/         # Widget 메인
    ├── components/
    │   ├── ui/
    │   └── checkout/       # 카드 폼
    └── lib/
        ├── api/
        └── validation/     # Luhn, Zod
```

**분리 이유**: 보안 격리, 독립 배포, iframe 통신 학습

---

## 기술 스택

- **Next.js 14 (App Router)**: 파일 기반 라우팅
- **TypeScript**: 타입 안전성
- **Tailwind CSS + shadcn/ui**: 컴포넌트 추가 간편
- **React Hook Form + Zod**: 선언적 폼 + validation
- **ky**: HTTP 클라이언트
- **MSW**: Mock API (백엔드 미구현 시)
- **Vitest + Playwright**: 단위/E2E 테스트

---

## 백엔드 API 설계

**⚠️ `/docs/openapi/` 참조**

### 변경 후 API

1. **POST /payments/ready** (변경 없음)
   - 결제 세션 생성
   - 응답: paymentKey, checkoutUrl, expiresAt

2. **POST /payments/checkout/submit** (새로 추가)
   - 결제수단 제출 + 저장 + 3DS 판단
   - 요청: payment_key, payment_method
   - 응답: **next_action** (challenge_3ds2 | none | ...)

3. **POST /payments/confirm** (역할 변경)
   - 최종 승인 (결제수단은 이미 저장됨)
   - 요청: payment_key만
   - 응답: status (DONE | WAITING_FOR_DEPOSIT)

### 2단계 프로세스
1. **submit**: 결제수단 저장 + 3DS 판단 (Authentication)
2. **confirm**: 카드사 승인 + 실제 결제 (Authorization)

---

## 전체 결제 플로우

**⚠️ successUrl은 완료 페이지가 아닌 confirm API 호출 중간 단계!**

```
1. 가맹점 상품 페이지
   └─ <iframe src="widget?paymentKey=xxx">

2. Widget iframe
   └─ 카드 입력 → submit API

3. submit 응답 (NextAction)
   ├─ 3DS 필요: challenge_3ds2 → 3DS 페이지
   └─ 3DS 불필요: none → successUrl

4. successUrl ⭐
   ├─ 쿼리 파라미터 추출 (paymentKey, amount)
   ├─ 금액 검증 (보안!)
   └─ confirm API 호출 (10분 이내)

5. confirm 응답
   ├─ 카드: DONE → 결제 완료
   └─ 가상계좌: WAITING_FOR_DEPOSIT → 계좌 발급
```

---

## 상품 데모 시나리오

**파일**: `merchant-demo/lib/mock-products.ts`

### 기본 시나리오

1. 일반 상품 (10,000원): 3DS 없음
2. 고가 상품 (50,000원): 3DS 필수
3. 최소 금액 (500원): 결제 실패
4. 할부 전용 (100,000원): 할부만 가능
5. 결제 실패 (20,000원): 의도적 실패

**확장**: scenario 메타데이터로 동작 제어

---

## 구현 단계

### Phase 1: 프로젝트 셋업
- merchant-demo, payment-widget 생성
- 패키지 설치 (ky, react-hook-form, zod, shadcn/ui)
- MSW, Vitest, Playwright 설정

### Phase 2: 가맹점 데모
- Mock 상품 데이터
- 상품 목록/상세 페이지
- API 클라이언트 (ready 함수)

### Phase 3: Payment Widget
- Widget 메인 페이지
- Luhn 알고리즘 + Zod 스키마
- CardPaymentForm (입력/포맷팅/검증)
- submit API 연동

### Phase 4: successUrl + confirm
- successUrl 페이지 (쿼리 파라미터 추출)
- 금액 검증
- confirm API 호출
- 완료/실패 UI

### Phase 5: 3DS 인증 (선택)
- 3DS 페이지 (ACS iframe)
- 자동 form submit
- 타임아웃 처리

---

## Task/SubTask 분할

**Git 브랜치**: `feature/<task-id>-<task-name>` (CLAUDE.md 참조)

### Task 0: 백엔드 API 문서 업데이트 (OpenAPI)
- **브랜치**: `feature/T0-api-spec-update` (base: `feature/checkout-page`)
- **SubTasks**:
  - T0.1: `/docs/openapi/api/payment.yaml` - submit 엔드포인트 추가
  - T0.2: `/docs/openapi/api/payment.yaml` - confirm 엔드포인트 수정
  - T0.3: `/docs/openapi/schemas/payment.yaml` - NextAction 스키마 추가
  - T0.4: `/docs/openapi/schemas/payment.yaml` - PaymentMethod 스키마 추가
- **테스트 계획**:
  - OpenAPI validator로 스펙 검증
  - Swagger UI로 문서 확인

### Task 1: 프로젝트 셋업
- **브랜치**: `feature/T1-project-setup` (base: `feature/T0-api-spec-update`)
- **SubTasks**:
  - T1.1: merchant-demo 생성
  - T1.2: payment-widget 생성
  - T1.3: 패키지 설치
  - T1.4: 환경 변수 설정
  - T1.5: MSW 설정
  - T1.6: Vitest/Playwright 설정
- **테스트 계획**:
  - 양쪽 프로젝트 `npm run dev` 실행 확인
  - MSW 활성화 확인 (브라우저 콘솔)
  - `npm run test` 통과
- **완료 확인**:
  - [ ] http://localhost:3000, 3001 접속
  - [ ] 브라우저 콘솔에 MSW 메시지 확인

### Task 2: 가맹점 데모
- **브랜치**: `feature/T2-merchant-demo` (base: `feature/T1-project-setup`)
- **SubTasks**:
  - T2.1: Mock 상품 데이터
  - T2.2: 상품 목록 페이지
  - T2.3: ProductCard 컴포넌트
  - T2.4: 상품 상세 페이지
  - T2.5: API 클라이언트 구조
  - T2.6: ready API 함수
- **테스트 계획**:
  - **단위**: `lib/api/client.test.ts`, `lib/api/payments.test.ts`
  - **컴포넌트**: `components/merchant/ProductCard.test.tsx`
  - **커버리지**: API 90%+
- **완료 확인**:
  - [ ] http://localhost:3000/merchant → 5개 상품 표시
  - [ ] 상품 클릭 → 상세 페이지 이동
  - [ ] `npm run test:coverage` → 90%+

### Task 3: Payment Widget
- **브랜치**: `feature/T3-payment-widget` (base: `feature/T2-merchant-demo`)
- **SubTasks**:
  - T3.1: Widget 메인 페이지
  - T3.2: Luhn 알고리즘
  - T3.3: Zod 스키마
  - T3.4: CardPaymentForm
  - T3.5: CardNumberInput (포맷팅)
  - T3.6: ExpiryDateInput, CvcInput
  - T3.7: submit API 연동
  - T3.8: 컴포넌트 테스트
- **테스트 계획**:
  - **단위**: Luhn (100% 필수), Zod 스키마
  - **컴포넌트**: CardPaymentForm
  - **커버리지**: Validation 100%, 컴포넌트 80%+
- **완료 확인**:
  - [ ] http://localhost:3001/widget?paymentKey=test 접속
  - [ ] 카드 입력 → 1234-5678-9012-3456 자동 포맷팅
  - [ ] 잘못된 카드 → 에러 메시지
  - [ ] Network 탭 submit 호출 확인
  - [ ] Luhn 커버리지 100%

### Task 4: successUrl + confirm
- **브랜치**: `feature/T4-success-url-confirm` (base: `feature/T3-payment-widget`)
- **SubTasks**:
  - T4.1: successUrl 페이지 구조
  - T4.2: 쿼리 파라미터 추출
  - T4.3: 금액 검증
  - T4.4: confirm API 함수
  - T4.5: 로딩/성공/실패 UI
  - T4.6: 테스트
- **테스트 계획**:
  - **단위**: 금액 검증 함수
  - **컴포넌트**: `app/success/page.test.tsx`
  - **통합**: Widget → submit → successUrl → confirm (MSW)
  - **커버리지**: 90%+
- **완료 확인**:
  - [ ] Widget 결제 → successUrl 리다이렉트
  - [ ] Network 탭 confirm 호출
  - [ ] 완료 화면 표시

### Task 5: 3DS 인증
- **브랜치**: `feature/T5-3ds-challenge` (base: `feature/T4-success-url-confirm`)
- **SubTasks**:
  - T5.1: 3DS 페이지 구조
  - T5.2: ACS iframe 렌더링
  - T5.3: 자동 form submit
  - T5.4: 타임아웃 처리
- **테스트 계획**:
  - **컴포넌트**: `app/3ds/page.test.tsx`
  - **커버리지**: 70%+
- **완료 확인**:
  - [ ] 고가 상품 (5555...) → 3DS 페이지 이동
  - [ ] iframe 렌더링
  - [ ] 3분 타임아웃 동작

### Task 6: E2E 테스트
- **브랜치**: `feature/T6-e2e-tests` (base: `feature/T5-3ds-challenge`)
- **SubTasks**:
  - T6.1: 카드 결제 (3DS 없음)
  - T6.2: 카드 결제 (3DS 있음)
  - T6.3: 결제 실패
  - T6.4: 금액 검증 실패
- **테스트 계획**:
  - **E2E 시나리오**: 4개 (3DS 없음/있음, 실패, 금액 검증)
  - **실행**: MSW, Headless/UI 모드
- **완료 확인**:
  - [ ] merchant-demo, payment-widget 동시 실행
  - [ ] `npm run test:e2e` 전체 통과
  - [ ] 4개 시나리오 모두 구현

---

## Git 브랜치 전략

### ⚠️ 순차적 브랜치 생성 (필수)

Task가 의존 관계이므로 **반드시 직전 브랜치에서 생성**:

```bash
git checkout feature/T0-api-spec-update
git checkout -b feature/T1-project-setup  # T0에서 생성 ✅

git checkout feature/T1-project-setup
git checkout -b feature/T2-merchant-demo  # T1에서 생성 ✅
```

**PR base도 직전 Task로 설정**:
```bash
gh pr create --base feature/T1-project-setup --title "[Task 2] ..."
```

### ❌ 잘못된 예
```bash
# 모든 브랜치를 checkout-page에서 생성 ❌
git checkout feature/checkout-page
git checkout -b feature/T2-merchant-demo  # T1 없어서 작업 불가!
```

---

## 작업 프로세스

### 시작
1. PRD 해당 Task 섹션 읽기 (SubTask, 테스트 계획, 체크리스트)
2. 직전 브랜치에서 생성: `git checkout feature/T<이전> && git checkout -b feature/T<현재>`

### 진행
- SubTask 단위 커밋
- 코드 작성 후 즉시: `npm run dev` (브라우저 확인) + `npm run build` + `npm run test`

### 완료
1. 해당 Task의 **완료 체크리스트** 전체 확인
2. `npm run test:coverage` → 목표 달성 확인
3. PR 생성 (base는 직전 Task)

---

## Task 공통 완료 체크리스트

모든 Task 완료 전 확인:
- [ ] 모든 SubTask 코드 완료
- [ ] PRD 테스트 계획 항목 전부 완료
- [ ] `npm run dev` → 브라우저 수동 확인 (콘솔 에러 없음)
- [ ] `npm run test` 전체 통과
- [ ] `npm run test:coverage` → 목표 달성
- [ ] `npm run build` 성공
- [ ] ESLint 경고 없음

---

## 확장성 고려사항

### 새 결제수단 추가 (가상계좌)
1. OpenAPI 스펙 추가
2. `lib/api/types.ts` 타입 추가
3. `components/checkout/VirtualAccountForm.tsx` 생성
4. Widget에서 결제수단 분기

### 여러 가맹점 지원
1. Mock 데이터 → DB 조회
2. merchant_id별 필터링

### 모바일 앱
1. iframe 제거
2. WebView에서 Widget 직접 로드
3. deep link 사용

---

## 향후 확장 계획

### 추가 결제수단
- 가상계좌
- 간편결제 (카카오페이, 네이버페이)
- 해외카드

### 추가 기능
- 영수증 PDF
- 결제 내역 조회
- 환불/취소
- Webhook 테스트

### 기술 개선
- React Query (서버 상태)
- 성능 모니터링
- 접근성 (ARIA)

---

## 참고 자료

- **OpenAPI 스펙** (Source of Truth): `/docs/openapi/`
- **CLAUDE.md**: 개발 원칙, 테스트 전략, Git 전략
- **README.md**: 실행 방법, 테스트 카드
- Next.js, shadcn/ui, React Hook Form, Zod, ky, MSW, Vitest, Playwright 공식 문서
