# PayBuddy Frontend

자체 PG 서버의 결제 플로우를 학습하기 위한 프론트엔드 프로젝트

## 프로젝트 구성

- **merchant-demo** (port 3000): 가맹점 데모 페이지 (상품 목록, 결제 요청)
- **payment-widget** (port 3001): PG 결제 Widget (iframe으로 제공되는 카드 입력 UI)

## 빠른 시작

### 1. 백엔드 실행
```bash
cd ../payment
./gradlew bootRun
# → http://localhost:8080
```

### 2. 가맹점 실행
```bash
cd merchant-demo
npm install
npm run dev
# → http://localhost:3000
```

### 3. Widget 실행
```bash
cd payment-widget
npm install
npm run dev -- -p 3001
# → http://localhost:3001
```

## 테스트 시나리오

1. **http://localhost:3000/merchant** 접속
2. 테스트할 상품 선택:
   - **일반 상품 (10,000원)**: 3DS 인증 없음
   - **고가 상품 (50,000원)**: 3DS 인증 필수
   - **최소 금액 테스트 (500원)**: 결제 실패 (1,000원 미만)
   - **할부 전용 상품 (100,000원)**: 할부만 가능
   - **결제 실패 테스트 (20,000원)**: 의도적 실패
3. Widget iframe에서 테스트 카드 정보 입력
4. 결제 플로우 확인

## 테스트 카드

| 카드번호 | 유효기간 | CVC | 시나리오 |
|---------|---------|-----|---------|
| 4111-1111-1111-1111 | 12/25 | 123 | ✅ 성공 (3DS 없음) |
| 5555-5555-5555-4444 | 12/25 | 123 | ✅ 성공 (3DS 필수) |
| 4000-0000-0000-0002 | 12/25 | 123 | ❌ 실패 (잔액 부족) |
| 4000-0000-0000-0069 | 12/25 | 123 | ❌ 실패 (만료) |

## 아키텍처

**Widget (iframe) + Redirect (successUrl)** 방식

```
가맹점 페이지 (Widget iframe 임베드)
  ↓
submit API (결제수단 제출 + 3DS 판단)
  ↓
3DS 인증 (필요 시)
  ↓
successUrl (리다이렉트)
  ↓
confirm API (최종 승인)
  ↓
완료 화면
```

상용 PG사의 일반적인 방식을 따름 (UI는 토스페이먼츠 참고)

## 기술 스택

- **Next.js 14** (App Router)
- **TypeScript**
- **Tailwind CSS + shadcn/ui**
- **React Hook Form + Zod**
- **ky** (HTTP 클라이언트)

## 디렉토리 구조

```
frontend/
├── merchant-demo/              (가맹점, port 3000)
│   ├── app/
│   │   ├── merchant/          # 상품 목록, 상세
│   │   └── success/           # successUrl 처리
│   ├── components/
│   └── lib/
│       ├── api/               # API 클라이언트
│       └── mock-products.ts   # 테스트 시나리오
│
└── payment-widget/             (Widget, port 3001)
    ├── app/
    │   └── widget/            # Widget 메인
    ├── components/
    │   └── checkout/          # 카드 입력 폼
    └── lib/
        └── validation/        # Luhn 알고리즘
```

## 환경 변수

### merchant-demo/.env.local
```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/v1
NEXT_PUBLIC_WIDGET_URL=http://localhost:3001/widget
NEXT_PUBLIC_SUCCESS_URL=http://localhost:3000/success
```

### payment-widget/.env.local
```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/v1
```

## 주요 API

### POST /payments/ready
결제 세션 생성

### POST /payments/checkout/submit ⭐ (새로 추가)
결제수단 제출 + 3DS 판단

### POST /payments/confirm ⭐ (수정됨)
최종 승인 (결제수단 파라미터 제거)

## 개발 가이드

상세한 개발 가이드는 [CLAUDE.md](./CLAUDE.md) 참고

## PRD 문서

상세한 요구사항 및 구현 계획은 [PRD.md](./PRD.md) 참고

## 주의사항

- **포트**: merchant(3000), widget(3001), backend(8080)
- **CORS**: 백엔드에서 3000, 3001 포트 허용 필요
- **환경 변수**: NEXT_PUBLIC_ prefix 필수
- **테스트 시나리오**: `mock-products.ts` 수정으로 쉽게 추가 가능

## 문제 해결

### CORS 에러
백엔드에서 CORS 설정 확인

### Widget iframe 로드 안 됨
- Widget 서버 실행 확인 (port 3001)
- .env.local 설정 확인

### API 호출 실패
- 백엔드 서버 실행 확인
- 네트워크 탭에서 요청/응답 확인
- API 엔드포인트 확인 (/checkout/submit, /confirm)

## 라이선스

MIT
