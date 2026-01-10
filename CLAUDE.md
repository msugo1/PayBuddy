# Claude Code Instructions

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines, treat as if import is in the main CLAUDE.md file.**
@./.taskmaster/CLAUDE.md

## 워크플로우
- task 시작 전: 복잡하면 subtask 분할 (`expand --id=<id> --research`)
- commit: subtask 없으면 task당 1개, 있으면 subtask당 1개

## 커밋 메시지
- **Conventional Commit 준수**: `type: 제목`
- **간결하게**: 설명 필요한 경우만 body 작성
- **types**: feat, fix, docs, refactor, test, chore

## 코딩 컨벤션

### 제어문
- **if-else는 항상 중괄호 사용**: 단일 표현식이라도 `{ }` 필수
```kotlin
// ✅ 올바른 방식
if (condition) {
    return true
}

// ❌ 금지
if (condition) return true
```

## 단위 테스트 가이드

### 테스트 범위
- **Sociable 단위 테스트**: DI 컨테이너 없이, 테스트 대상과 협력자를 함께 테스트 (기본)
- **Solitary 단위 테스트**: 복잡한 알고리즘은 단독 테스트 가능 (조건 분기, 복잡한 계산 로직)

### FIRST 원칙
- **Fast**: 빠른 피드백
- **Isolated**: 작은 코드에 집중, 다른 테스트와 독립 실행
- **Repeatable**: 매 실행 동일 결과 (시간 의존 금지: `now()`)
- **Self-validating**: 단언문으로 검증
- **Timely**: 적절한 시점 작성

### 4대 요소
1. **회귀방지**: 복잡한 비즈니스 로직 우선 테스트
2. **리팩토링 내성**: 구현 변경 시 테스트 깨지지 않음 (필수, 0 or 1)
3. **빠른 피드백** ↔ **회귀방지**: trade-off 관계
4. **유지보수성**: 읽기 쉽고 실행하기 쉬운 테스트

### 금지 사항
- ❌ 상수 검증 테스트 (예: `PaymentPolicy.MIN_AMOUNT == 1000`)
- ❌ 테스트 내 if 분기문
- ❌ 여러 실행/검증 구절 (한 테스트 = 한 동작)
- ❌ 과도한 mock (최종 결과 검증 우선)
- ❌ 범용 픽스처 (테스트별 구체적 픽스처)
- ❌ 지나치게 민감한 단언 (예: 예외 메시지 매칭)

### Given-When-Then 패턴
- **Given**: 준비만, 검증(assertion) 금지. 크면 팩터리 메서드 분리
- **When**: 보통 한 줄, 두 줄 이상이면 캡슐화 위반 의심
- **Then**: 여러 결과 모두 검증, 크면 커스텀 equals 사용
- 각 섹션마다 `// Given`, `// When`, `// Then` 주석 표기 후 개행

### 검증 방식
- **VO 단위 검증**: equals로 객체 단위 검증 (프로퍼티 하나씩 금지)
- **Factory/Mapper 테스트**: 모든 프로퍼티 매핑 검증 (의도대로 생성/할당)
- **예외 검증**: 타입만 검증 (`.hasMessageContaining()` 금지)
- **require vs check**: 인자 검증 → `require`, 상태 검증 → `check`

### 테스트 대상
- ✅ **객체 간 협력**으로 만들어지는 도메인 규칙
- ✅ **복잡한 비즈니스 로직** (조건 분기, 계산, 상태 전이)
- ✅ 복잡한 알고리즘
- ❌ 단일 객체의 단순 validation (isNotBlank, > 0) - 상위 레벨에서 커버되면 스킵
  - 단, validation 자체를 검증하는 테스트는 예외
- ❌ getter/setter, 상수 검증

## 주석 작성 가이드

### 원칙
- **코드로 표현 가능하면 주석 쓰지 마라** (변수명/함수명으로 해결)
- **What이 아닌 Why를 설명**
- **자명한 KDoc 주석 생략** (단순 클래스명 번역, @param id, @property name 등)

### ❌ 금지
- @property, 번역, 클래스명 설명
- What 나열 (`/** 검증 및 계산 담당 */` ← 코드 보면 앎)

### ✅ 작성
- **Why&도메인 규칙**: 코드로 설명되지 않는 중요한 부분(ex. 비즈니스 규칙, 법적 제약, 외부 스펙)
- **향후계획**: 왜 추상화했는지, 확장 방향
- **TODO**: 기술부채, 설계 고민

### 예시
```kotlin
// ❌
@property total 총 금액
/** 정책 검증 및 만료시간 계산 담당 */ class Factory  // What 나열

// ✅
/** 같은 주문 재시도 시 금액 변조 확인 */ fun isIdenticalPayment()  // Why
/** merchantId + orderId로 중복 요청 방지 */ class PaymentSession  // 도메인 규칙
/** 향후 상점별 차등 정책 확장 예정 */ interface PaymentPolicy  // 확장 계획
// TODO: validation 위치 - API vs Domain 불변성 보장  // 기술부채
```
