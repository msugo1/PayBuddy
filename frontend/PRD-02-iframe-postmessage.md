# PRD: iframe + postMessage 통신 구현 - 남은 작업

## 📌 현재 상태

### 완료된 작업 (feature/T2-widget-postmessage-send 브랜치)

✅ **Task 0: postMessage 타입 정의** (커밋: a171c87)
- PaymentMessage, ThreeDSMessage 인터페이스
- Type guard 함수 + 단위 테스트 28개
- 파일: merchant-demo/lib/types/postmessage.ts, payment-widget/lib/types/postmessage.ts

✅ **Task 1: merchant-demo iframe 렌더링** (커밋: 25a79b4)
- window.location.href 제거 → iframe 렌더링 + postMessage 리스너
- 파일: merchant-demo/app/merchant/product/[id]/page.tsx

✅ **Task 2: payment-widget postMessage 발신** (커밋: c5ca23a)
- window.top.location.href 제거 → window.parent.postMessage
- 파일: payment-widget/app/widget/page.tsx, payment-widget/app/3ds/page.tsx

---

## ❌ 남은 작업

### Task 3: E2E 테스트 수정

**목적**: iframe + postMessage 방식으로 변경된 결제 플로우에 맞춰 E2E 테스트 수정

**현재 상태 (Redirect 방식)**:
- 결제하기 버튼 클릭 → 전체 페이지가 /widget으로 redirect
- page.locator()로 widget 요소 접근
- 결제 완료 → /success로 redirect

**변경 목표 (iframe 방식)**:
- 결제하기 버튼 클릭 → 페이지는 그대로, iframe 모달 렌더링
- frameLocator()로 iframe 내부 요소 접근
- postMessage 통신 후 → /success로 redirect

**핵심 변경 포인트**:
- URL 검증 제거: 페이지는 /merchant/product/[id]에 유지
- iframe 모달 렌더링 확인 추가
- `page.locator()` → `page.frameLocator('iframe[src*="widget"]').locator()`

**수정 파일**:
- frontend/merchant-demo/e2e/card-payment-no-3ds.spec.ts

**완료 조건**:
- [ ] iframe 렌더링 테스트 통과
- [ ] 카드 결제 플로우 (3DS 없음) 테스트 통과
- [ ] `npm run test:e2e` 전체 통과

---

### Task 4: 문서 업데이트

**목적**: README.md, CLAUDE.md에 iframe + postMessage 통신 방식 반영

#### 1. CLAUDE.md 수정

**수정 위치**: line 55-61 "### 2. Widget 통신" 섹션

**현재 내용**:
- "Redirect 방식 (postMessage 아님)"
- window.top.location.href 사용

**변경 내용**:
- "iframe + postMessage 방식"
- postMessage 타입 정의 (PaymentMessage, ThreeDSMessage)
- 메시지 흐름: widget → parent 간 통신
- 보안: origin 검증, type guard

**참고할 구현 파일**:
- lib/types/postmessage.ts (타입 정의)
- merchant-demo/app/merchant/product/[id]/page.tsx (리스너 구현)
- payment-widget/app/widget/page.tsx (발신 구현)

---

#### 2. README.md 수정

**수정 위치**: line 56-72 "## 아키텍처" 섹션

**현재 내용**:
- "Widget (iframe) + Redirect (successUrl)" 방식
- 단순한 직선형 플로우

**변경 내용**:
- iframe + postMessage 통신 흐름 다이어그램
- postMessage 타입별 설명 테이블:
  - payment_completed: 3DS 없이 결제 완료
  - 3ds_required: 3DS 인증 필요
  - payment_error: 결제 실패
  - threeds_completed: 3DS 인증 완료
- iframe 보안 가이드 (origin 검증, type guard, CORS)

---

**완료 조건**:
- [ ] CLAUDE.md Widget 통신 섹션 업데이트
- [ ] README.md 아키텍처 섹션 업데이트
- [ ] 문서 내용이 실제 구현과 일치
- [ ] postMessage 타입 정의 정확성 확인

---

## 📝 충돌 가능성

다음 파일들이 feature/T5-3ds-challenge와 feature/T2-widget-postmessage-send 양쪽에서 수정됨:
- merchant-demo/app/merchant/product/[id]/page.tsx
- payment-widget/app/3ds/page.tsx

**충돌 해결 방향**: postMessage 로직(T2) 우선 유지
