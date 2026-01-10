package com.paybuddy.payment.domain

import java.time.Instant
import kotlin.math.min

// TODO: 결제수단 확장 시 PromotionCriteria를 sealed class로 리팩토링
//       - sealed interface PromotionCriteria
//       - CardPromotionCriteria, VirtualAccountPromotionCriteria, EasyPayPromotionCriteria
//       - matches() 내부에서 when (paymentDetails) + smart cast 활용
//       현재는 1차 카드만 구현하여 nullable 필드로 단순화
data class Promotion(
    val id: String,
    val name: String,
    val provider: PromotionProvider,
    val discountType: DiscountType,
    val discountValue: Long,  // FIXED: 할인 금액(원), PERCENTAGE: 할인 비율(%)
    val maxDiscountAmount: Long?,

    // 카드 조건 (1차: 카드만 구현)
    val cardBrand: CardBrand?,
    val cardType: CardType?,
    val issuerCode: String?,
    val minAmount: Long?,

    val validFrom: Instant,
    val validUntil: Instant,
) {
    init {
        if (discountType == DiscountType.FIXED && maxDiscountAmount != null) {
            require(discountValue <= maxDiscountAmount) {
                "FIXED 타입에서 discountValue는 maxDiscountAmount 이하여야 합니다"
            }
        }

        require(cardBrand != null || cardType != null || issuerCode != null || minAmount != null) {
            "최소 하나의 적용조건이 필요합니다"
        }
    }
    fun matches(card: Card?, amount: Long): Boolean {
        if (card == null) {
            return false
        }

        if (cardBrand != null && card.brand != cardBrand) {
            return false
        }
        if (cardType != null && card.cardType != cardType) {
            return false
        }
        if (issuerCode != null && card.issuerCode != issuerCode) {
            return false
        }
        if (minAmount != null && amount < minAmount) {
            return false
        }

        return true
    }

    fun calculateDiscount(amount: Long): Long {
        return when (discountType) {
            DiscountType.FIXED -> min(discountValue, maxDiscountAmount ?: Long.MAX_VALUE)
            DiscountType.PERCENTAGE -> {
                val calculated = amount * discountValue / 100
                min(calculated, maxDiscountAmount ?: Long.MAX_VALUE)
            }
        }
    }

    fun isIssuerDrivenPromotion(): Boolean {
        return provider == PromotionProvider.CARD_ISSUER
    }
}

enum class DiscountType {
    FIXED,
    PERCENTAGE
}
