package com.paybuddy.payment.domain

/**
 * 프로모션 최적화 알고리즘 추상화
 *
 * ## 왜 필요한가?
 * 0-1 Knapsack 문제는 여러 알고리즘으로 해결 가능 (DP, MITM, Greedy 등)
 * 각 알고리즘은 성능 특성이 다르므로, 상황에 따라 교체 가능하도록 인터페이스로 분리
 *
 * ## 언제 어떤 구현체를 사용할까?
 * - Knapsack (DP): capacity 작을 때 유리, 카드사 프로모션 개수 우선
 * - MeetInTheMiddle: 프로모션 개수 적고 capacity 클 때 유리, 순수 할인 최대화
 *
 * ## 향후 확장
 * - Greedy: 근사해로 충분한 경우
 * - Branch & Bound: 정확한 해가 필요하지만 DP보다 빠른 경우
 */
interface PromotionOptimizer {
    /**
     * 주어진 프로모션 목록에서 최적의 조합을 선택
     *
     * @param promotions 선택 가능한 프로모션 목록 (이미 매칭 조건을 만족하는 프로모션만 전달됨)
     * @param originalAmount 원 결제 금액 (PERCENTAGE 타입 할인 계산에 사용)
     * @param maxDiscountLimit 최대할인가능금액 (= originalAmount - minPaymentAmount)
     * @return 최적화된 프로모션 조합 (할인 합계가 maxDiscountLimit 이하이면서 최대가 되는 조합)
     */
    fun optimize(
        promotions: List<Promotion>,
        originalAmount: Long,
        maxDiscountLimit: Long
    ): List<Promotion>
}