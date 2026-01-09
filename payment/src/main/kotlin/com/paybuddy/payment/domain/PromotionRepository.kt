package com.paybuddy.payment.domain

interface PromotionRepository {
    fun findActivePromotions(paymentMethodType: PaymentMethodType): List<Promotion>
}
