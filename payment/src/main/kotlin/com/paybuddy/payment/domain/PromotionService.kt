package com.paybuddy.payment.domain

/**
 * 프로모션 조회 및 적용 조건 검증 서비스
 */
interface PromotionService {
    /**
     * 적용 가능한 프로모션 계산
     *
     * @return 조건을 만족하는 프로모션 목록 (최대 할인 금액 순)
     */
    fun calculateEffectivePromotions(
        merchantId: String,
        card: Card,
        amount: Long
    ): List<EffectivePromotion>
}
