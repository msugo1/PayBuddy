package com.paybuddy.payment.service

import com.paybuddy.payment.api.model.PaymentReadyRequest
import com.paybuddy.payment.api.model.PaymentReadyResponse

interface PaymentOperations {
    fun readyPayment(
        idempotencyKey: String,
        request: PaymentReadyRequest
    ): PaymentReadyResponse
}
