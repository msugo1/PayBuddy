package com.paybuddy.payment.domain

import jakarta.persistence.*
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.min

// TODO: 결제수단 확장 시 PromotionCriteria를 sealed class로 리팩토링
//       - sealed interface PromotionCriteria
//       - CardPromotionCriteria, VirtualAccountPromotionCriteria, EasyPayPromotionCriteria
//       - matches() 내부에서 when (paymentDetails) + smart cast 활용
//       현재는 1차 카드만 구현하여 nullable 필드로 단순화
@Entity
@Table(name = "promotion")
class Promotion(
    @Id
    @Column(length = 50)
    val id: String,

    @Column(nullable = false, length = 100)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val provider: PromotionProvider,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "discount_type")
    val discountType: DiscountType,

    @Column(nullable = false, name = "discount_value")
    val discountValue: Long,  // FIXED: 할인 금액(원), PERCENTAGE: 할인 비율(%)

    @Column(name = "max_discount_amount")
    val maxDiscountAmount: Long?,

    // 카드 조건 (1차: 카드만 구현)
    @Enumerated(EnumType.STRING)
    @Column(name = "card_brand")
    val cardBrand: CardBrand?,

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type")
    val cardType: CardType?,

    @Column(name = "issuer_code")
    val issuerCode: String?,

    @Column(name = "min_amount")
    val minAmount: Long?,

    @Column(nullable = false, name = "valid_from")
    val validFrom: Instant,

    @Column(nullable = false, name = "valid_until")
    val validUntil: Instant,

    @Column(nullable = false, updatable = false, name = "created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(nullable = false, name = "updated_at")
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
    init {
        if (discountType == DiscountType.FIXED && maxDiscountAmount != null) {
            require(discountValue <= maxDiscountAmount) {
                "FIXED 타입에서 discountValue는 maxDiscountAmount 이하여야 합니다"
            }
        }

        require(cardBrand != null || cardType != null || issuerCode != null) {
            "카드 조건(cardBrand, cardType, issuerCode) 중 최소 하나는 필수입니다"
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

    @PreUpdate
    fun preUpdate() {
        this.updatedAt = OffsetDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Promotion) {
            return false
        }
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class DiscountType {
    FIXED,
    PERCENTAGE
}
