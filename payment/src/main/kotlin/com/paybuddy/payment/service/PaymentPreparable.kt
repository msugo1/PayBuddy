package com.paybuddy.payment.service

import com.paybuddy.payment.domain.OrderLine
import com.paybuddy.payment.domain.PaymentSession

interface PaymentPreparable {
    fun prepare(
        merchantId: String,
        orderId: String,
        orderLine: OrderLine,
        totalAmount: Long,
        supplyAmount: Long,
        vatAmount: Long,
        successUrl: String,
        failUrl: String
    ): PaymentSession
}
