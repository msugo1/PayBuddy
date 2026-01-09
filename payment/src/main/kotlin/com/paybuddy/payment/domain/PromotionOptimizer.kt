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
    fun optimize(
        promotions: List<Promotion>,
        originalAmount: Long,
        capacity: Long
    ): List<Promotion>
}