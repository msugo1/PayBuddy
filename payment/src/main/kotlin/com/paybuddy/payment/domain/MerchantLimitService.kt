package com.paybuddy.payment.domain

interface MerchantLimitService {
    fun check(merchantId: String, paymentMethod: PaymentMethodType, amount: Long): Boolean
    fun consume(merchantId: String, paymentId: String, amount: Long)
    fun restore(merchantId: String, paymentId: String, amount: Long)
}
