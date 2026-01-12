package com.paybuddy.payment.domain.merchant

import com.paybuddy.payment.domain.PaymentMethodType
import com.paybuddy.payment.domain.PaymentPolicy

interface MerchantLimitService {
    fun check(merchantId: String, paymentMethod: PaymentMethodType, amount: Long): Boolean
    fun consume(merchantId: String, paymentId: String, amount: Long)
    fun restore(merchantId: String, paymentId: String, amount: Long)
}
