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

## 단위 테스트 가이드

### 테스트 범위
- Sociable 단위 테스트: DI 컨테이너 없이, 테스트 대상과 협력자를 함께 테스트

### FIRST 원칙
- **Fast**: 빠른 피드백 제공
- **Isolated**: 작은 코드에 집중, 다른 테스트와 독립적 실행
- **Repeatable**: 실행할 때마다 동일한 결과 (시간 의존 금지: `now()`)
- **Self-validating**: 반드시 단언문으로 검증
- **Timely**: 적절한 시점에 작성

### 4대 요소
1. **회귀방지**: 복잡한 비즈니스 로직 우선 테스트
2. **리팩토링 내성**: 구현 변경 시 테스트 깨지지 않음 (필수, 0 or 1)
3. **빠른 피드백** ↔ **회귀방지**: trade-off 관계
4. **유지보수성**: 읽기 쉽고 실행하기 쉬운 테스트

### 금지 사항
- ❌ 상수 검증 테스트 (예: `PaymentPolicy.MIN_AMOUNT == 1000`)
- ❌ 테스트 내 if 분기문
- ❌ 여러 실행/검증 구절 (한 테스트 = 한 동작)
- ❌ 과도한 mock: 최종 결과 대신 mock 상호작용 검증
- ❌ 범용 픽스처: 테스트별 구체적 픽스처 사용
- ❌ 지나치게 민감한 단언 (예: 예외 메시지 정확히 매칭)

### AAA 패턴 원칙
- **Arrange**: 크면 팩터리 메서드 분리
- **Act**: 보통 한 줄, 두 줄 이상이면 캡슐화 위반 의심
- **Assert**: 여러 결과 모두 검증, 크면 커스텀 equals 사용

### 테스트 대상
- ✅ **객체 간 협력**으로 만들어지는 도메인 규칙
- ✅ **복잡한 비즈니스 로직** (조건 분기, 계산, 상태 전이)
- ✅ 복잡한 알고리즘
- ❌ 단일 객체의 단순 validation (isNotBlank, > 0, 등식 검증)
- ❌ getter/setter, 상수 검증

## 주석 작성 가이드

### 원칙
- **코드로 표현 가능하면 주석 쓰지 마라** (변수명/함수명으로 해결)
- **What이 아닌 Why를 설명**
- **자명한 KDoc 태그 생략** (@param id, @property name 등)

### ❌ 작성 금지
- 코드 중복: `// i에 1을 더한다`
- 번역 수준: `@property id 아이디`
- 나쁜 코드 변명: `// 복잡하지만 어쩔 수 없음`

### ✅ 작성 권장
- **Why**: 왜 이렇게 구현했는지 (성능, 제약사항)
- **도메인 규칙**: 비즈니스 규칙, 법적 제약, 외부 스펙
- **복잡한 패턴**: 정규식, 형식 등은 예시 포함
- **출처**: 복사한 코드, 참조 링크
- **TODO**: 미완성/기술부채 표시

### 예시
```kotlin
// ❌
/**
 * @property id 아이디
 */
val id: Long

// ✅
fun add(item: Item) {
    list.add(item)
    // 정렬 유지 - isInStock()에서 이진 검색 사용 (성능 최적화)
    Collections.sort(list)
}

/**
 * 신용점수 기반 대출심사 규칙
 *
 * 금융감독원 가이드라인(2023.05)에 따라
 * 대출상품별 신용점수 구간 검증 필수
 */
class CreditScoreRule
```
