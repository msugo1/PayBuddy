package com.paybuddy.payment.domain

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(name = "payment")
class Payment(
    @Id
    @Column(length = 26)
    val id: String,

    @Column(nullable = false, length = 26)
    val paymentKey: String,

    @Column(nullable = false)
    val merchantId: String,

    status: PaymentStatus,

    @Version
    val version: Long = 0,

    @Column(nullable = false)
    val originalAmount: Long,

    @JdbcTypeCode(SqlTypes.JSON) @Column(
        name = "effective_promotions",
        columnDefinition = "jsonb"
    )
    private val effectivePromotions: MutableList<EffectivePromotion> = mutableListOf(),

    @Embedded
    var cardPaymentDetails: CardPaymentDetails? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PaymentStatus = status
        private set

    val finalAmount: Long
        get() = originalAmount - effectivePromotions.sumOf { it.amount }

    fun addPromotion(promotion: EffectivePromotion, minPaymentAmount: Long) {
        require(promotion.amount > 0) { "프로모션 할인 금액은 0보다 커야 합니다" }
        require(finalAmount - promotion.amount >= minPaymentAmount) { "할인 적용 후 최종 금액은 최소 ${minPaymentAmount}원 이상이어야 합니다" }
        effectivePromotions.add(promotion)
    }

    fun submit(cardDetails: CardPaymentDetails) {
        cardPaymentDetails = cardDetails
    }

    fun fail(errorCode: String, failureReason: String) {
        requireNotNull(cardPaymentDetails) { "결제 수단 정보가 설정되지 않았습니다" }
        status = status.transitionTo(PaymentStatus.FAILED)
        cardPaymentDetails = cardPaymentDetails!!.copy(
            result = PaymentResult(errorCode = errorCode, failureReason = failureReason)
        )
    }

    fun requestAuthentication() {
        status = status.transitionTo(PaymentStatus.AUTHENTICATION_REQUIRED)
    }

    fun completeAuthentication() {
        status = status.transitionTo(PaymentStatus.PENDING_CONFIRM)
    }

    fun completeWithoutAuthentication() {
        status = status.transitionTo(PaymentStatus.PENDING_CONFIRM)
    }

    @PreUpdate
    fun preUpdate() {
        this.updatedAt = OffsetDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Payment) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
