package com.paybuddy.payment.domain

/**
 * ## 언제 사용?
 * - 프로모션 개수: 1~20개 (일반적인 경우)
 * - capacity: 10만원 이상 (금액이 클 때)
 * - 순수 할인 합계 최대화 (카드사 우선순위 불필요)
 *
 * ## 성능 비교 예시
 * 프로모션 10개, capacity 100,000원
 * - DP:   O(10 * 100,000) = 1,000,000 연산
 * - MITM: O(2^5) ≈ 64 연산 (15,000배 빠름!)
 */
object MeetInTheMiddlePromotionOptimizer : PromotionOptimizer {
    override fun optimize(
        promotions: List<Promotion>,
        originalAmount: Long,
        capacity: Long
    ): List<Promotion> {
        TODO()
    }
}