# Frontend 개발 가이드

## ⚠️ 최우선 원칙: OpenAPI가 Source of Truth

**모든 개발 전 `/docs/openapi/` 먼저 확인**

- `lib/api/types.ts`는 OpenAPI 스펙과 100% 일치
- API 변경 시 OpenAPI 스펙부터 수정
- NextAction, Payment 상태 등 모든 타입은 스펙 기반

**참조**:
- `/docs/openapi/api/payment.yaml` - 엔드포인트
- `/docs/openapi/schemas/payment.yaml` - 스키마

---

## 핵심 설계 원칙

1. **코드 최소화**: Next.js App Router (파일=URL), shadcn/ui, Zod
2. **명확한 구조**: 도메인별 분리, 단일 책임, 타입 안전성
3. **확장 가능**: 작은 컴포넌트, 중앙 API 관리, AI 친화적 패턴

---

## 프로젝트 구조

```
merchant-demo/ (port 3000)           payment-widget/ (port 3001)
├── app/                             ├── app/
│   ├── merchant/page.tsx            │   └── widget/page.tsx
│   ├── merchant/product/[id]/       ├── components/
│   └── success/page.tsx             │   ├── ui/         (shadcn)
├── components/                       │   └── checkout/  (카드 폼)
│   ├── ui/          (shadcn)        └── lib/
│   └── merchant/    (도메인)            ├── api/       (client, types)
└── lib/                                 └── validation/ (Luhn, Zod)
    ├── api/         (client, types)
    ├── mock-products.ts
    └── mocks/       (MSW)
```

---

## 핵심 개념

### 1. API 연동

**구조**:
- `lib/api/client.ts`: ky.create (timeout, retry, hooks)
- `lib/api/payments.ts`: ready, submit, confirm 함수
- `lib/api/types.ts`: ⚠️ OpenAPI 스펙 기반 타입 정의

**에러 처리**: Problem JSON → ApiError 클래스

### 2. Widget 통신

**패턴**: Redirect 방식 (postMessage 아님)
- submit API 호출 → NextAction 응답
- `next_action.type === 'challenge_3ds2'` → 3DS 페이지로 리다이렉트
- `next_action.type === 'none'` → successUrl로 리다이렉트
- `window.top.location.href` 사용 (iframe 전체 페이지 이동)

### 3. successUrl 처리 ⭐ 중요!

**역할**:
- ❌ 결제 완료 페이지가 아님
- ✅ confirm API를 호출하기 위한 중간 단계

**핵심 로직**:
1. 쿼리 파라미터에서 paymentKey, orderId, amount 추출
2. 금액 검증 (요청 금액 === 응답 금액) - 보안!
3. confirm API 호출 (10분 이내 필수, 안 하면 만료)
4. 결과에 따라 완료/실패 화면 표시

### 4. Validation

**Luhn 알고리즘**: `lib/validation/card-validator.ts` - 카드번호 검증
**Zod 스키마**: `lib/validation/schemas.ts` - 폼 validation
**React Hook Form**: zodResolver로 연동

### 5. MSW (백엔드 미구현 시)

**목적**: 백엔드 API 없이 독립 개발
**위치**: `src/mocks/handlers.ts`
**원칙**: ⚠️ OpenAPI 스펙 기반 Mock 응답

**활성화**:
- 브라우저: `app/layout.tsx`에서 `worker.start()`
- 테스트: `setupServer()` + beforeAll/afterAll

**시나리오 제어**: 테스트 카드번호로 3DS 필요/불필요, 에러 분기

---

## 주요 파일 & 역할

| 파일 | 역할 | 핵심 포인트 |
|------|------|------------|
| `lib/api/types.ts` | API 타입 정의 | ⚠️ OpenAPI 스펙 100% 일치 |
| `lib/api/client.ts` | HTTP 클라이언트 | ky, timeout, retry, Problem JSON 처리 |
| `lib/api/payments.ts` | API 함수 | ready, submit, confirm |
| `lib/validation/schemas.ts` | Zod 스키마 | cardPaymentSchema |
| `lib/validation/card-validator.ts` | Luhn 알고리즘 | isValidCardNumber |
| `lib/mock-products.ts` | 테스트 상품 | scenario 메타데이터 |
| `src/mocks/handlers.ts` | MSW 핸들러 | OpenAPI 기반 Mock |
| `app/success/page.tsx` | successUrl 처리 | confirm API 호출 (10분 이내) |
| `app/widget/page.tsx` | Widget 메인 | submit → NextAction 처리 |

---

## Next.js 필수 사항

1. **'use client'**: useState/useEffect/이벤트 핸들러 사용 시 파일 최상단에 필수
2. **환경 변수**: NEXT_PUBLIC_ prefix (브라우저에서 접근 가능)
3. **파일 기반 라우팅**: `app/merchant/page.tsx` → `/merchant`

---

## Git 브랜치 전략

**브랜치 네이밍**: `feature/<task-id>-<task-name>`
- 예: `feature/T1-project-setup`, `feature/T2.1-product-list-page`

**작업 흐름**:
1. main에서 feature 브랜치 생성
2. SubTask 단위로 커밋
3. Task 완료 후 PR → main에 merge

---

## 커밋 메시지 규칙

**형식**: `<type>: <subject>`

**Types**:
- `feat`: 새 기능
- `fix`: 버그 수정
- `test`: 테스트 추가/수정
- `refactor`: 리팩토링
- `docs`: 문서 수정
- `chore`: 빌드/설정 변경

**예시**:
```
feat: add Luhn algorithm validation
test: add CardPaymentForm component tests
fix: correct amount validation in successUrl
```

---

## 테스트 전략

### 테스트 철학
**리팩토링에 안전한 테스트** - 구현 세부사항이 아닌 동작 테스트

### 단위 테스트 (Vitest)
- ✅ **Validation 로직**: Luhn 알고리즘 (100% 커버리지 필수)
- ✅ **API 클라이언트**: ky, ready/submit/confirm 함수 (MSW 활용)
- ✅ **유틸리티**: 포맷팅, 금액 계산
- ❌ **단순 UI**: Presentational 컴포넌트 스킨

**파일 위치**:
- `lib/validation/card-validator.test.ts`
- `lib/api/payments.test.ts`

### 컴포넌트 테스트 (Testing Library)
- ✅ **폼 컴포넌트**: CardPaymentForm (입력/검증/제출)
- ✅ **상태 관리**: successUrl 페이지 confirm 로직
- ❌ **스타일링**: Tailwind 클래스명 변경에 깨지지 않도록

**파일 위치**:
- `components/checkout/CardPaymentForm.test.tsx`
- `app/success/page.test.tsx`

### E2E 테스트 (Playwright)
1. 카드 결제 (3DS 없음)
2. 카드 결제 (3DS 있음)
3. 결제 실패
4. 금액 검증 실패

**실행 환경**: MSW 활성화, Headless/UI 모드

**파일 위치**: `e2e/payment-flow.spec.ts`

### 커버리지 목표
- Validation: 100%
- API 클라이언트: 90%+
- 핵심 컴포넌트: 80%+
- 전체: 70%+

### 리팩토링 안전성 체크
테스트가 다음 변경에도 깨지지 않는지 확인:
- [ ] CSS 클래스명 변경
- [ ] 컴포넌트 내부 구조 변경
- [ ] API 응답 형식 변경 (OpenAPI 타입만 맞으면 OK)
- [ ] UI 라이브러리 교체

---

## PR 체크리스트

- [ ] 모든 SubTask 완료
- [ ] `npm run build` 성공
- [ ] `npm run test` 전체 통과
- [ ] ESLint 경고 없음
- [ ] 브라우저 콘솔 에러 없음
- [ ] 테스트 커버리지 목표 달성

---

## 테스트 카드

| 카드번호 | 유효기간 | CVC | 시나리오 |
|---------|---------|-----|---------|
| 4111-1111-1111-1111 | 12/25 | 123 | ✅ 성공 (3DS 없음) |
| 5555-5555-5555-4444 | 12/25 | 123 | ✅ 성공 (3DS 필수) |
| 4000-0000-0000-0002 | 12/25 | 123 | ❌ 실패 (잔액 부족) |
| 4000-0000-0000-0069 | 12/25 | 123 | ❌ 실패 (만료) |

---

## 환경 설정

### 환경 변수

**merchant-demo/.env.local**:
```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/v1
NEXT_PUBLIC_WIDGET_URL=http://localhost:3001/widget
NEXT_PUBLIC_SUCCESS_URL=http://localhost:3000/success
```

**payment-widget/.env.local**:
```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/v1
```

### CORS 설정 (백엔드)

백엔드에서 다음 origin 허용 필요:
- `http://localhost:3000` (merchant)
- `http://localhost:3001` (widget)

### 포트
- Backend: 8080
- Merchant: 3000
- Widget: 3001

---

## 확장 가이드

### 새 결제수단 추가 (예: 가상계좌)
1. OpenAPI 스펙에 스키마 추가
2. `lib/api/types.ts` 타입 추가
3. `components/checkout/VirtualAccountForm.tsx` 생성 (React Hook Form + Zod)
4. Widget에서 결제수단별 분기

### AI 개발 체크리스트
1. ✅ OpenAPI 스펙 먼저 확인
2. ✅ 기존 컴포넌트 패턴 유지
3. ✅ `npm run build`로 타입 체크
4. ✅ 브라우저 콘솔/Network 탭 확인

**추천 프롬프트**:
```
"/docs/openapi/schemas/payment.yaml의 [스키마명] 기반으로
TypeScript 타입과 Zod 스키마 생성. 기존 [참고파일]과 동일한 패턴 사용."
```

---

## 참고 자료

### 필수
- `/docs/openapi/` - API 스펙 (Source of Truth)
- `PRD.md` - 전체 요구사항, Task 분할, 구현 단계
- `README.md` - 실행 방법

### 기술 문서
- Next.js App Router, shadcn/ui, React Hook Form, Zod, ky, MSW, Vitest, Playwright
