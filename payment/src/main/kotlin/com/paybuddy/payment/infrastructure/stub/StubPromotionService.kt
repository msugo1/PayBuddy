package com.paybuddy.payment.infrastructure.stub

import com.paybuddy.payment.domain.*
import org.springframework.stereotype.Service

/**
 * 프로모션 서비스 Stub 구현 (1차)
 *
 * 고정된 프로모션 규칙:
 * 1. 플랫폼 프로모션: 10,000원 이상 시 1,000원 할인
 * 2. 카드사 프로모션 (VISA): 20,000원 이상 시 2,000원 할인
 */
@Service
class StubPromotionService : PromotionService {

    override fun calculateEffectivePromotions(
        merchantId: String,
        card: Card,
        amount: Long
    ): List<EffectivePromotion> {
        val promotions = mutableListOf<EffectivePromotion>()

        // 플랫폼 프로모션: 10,000원 이상
        if (amount >= 10_000) {
            promotions.add(
                EffectivePromotion(
                    name = "신규 회원 할인",
                    provider = PromotionProvider.PLATFORM,
                    amount = 1_000
                )
            )
        }

        // 카드사 프로모션: VISA 카드, 20,000원 이상
        if (card.brand == CardBrand.VISA && amount >= 20_000) {
            promotions.add(
                EffectivePromotion(
                    name = "VISA 카드 특별 할인",
                    provider = PromotionProvider.CARD_ISSUER,
                    amount = 2_000
                )
            )
        }

        return promotions.sortedByDescending { it.amount }
    }
}
