package com.paybuddy.payment.domain

data class EffectivePromotion(
    val name: String,
    val provider: PromotionProvider,
    val amount: Long,
)

enum class PromotionProvider {
    CARD_ISSUER, PLATFORM
}
