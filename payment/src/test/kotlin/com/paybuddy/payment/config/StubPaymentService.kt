package com.paybuddy.payment.config

import com.paybuddy.payment.api.IdempotencyConflictException
import com.paybuddy.payment.api.model.PaymentReadyRequest
import com.paybuddy.payment.api.model.PaymentReadyResponse
import com.paybuddy.payment.service.PaymentOperations
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Contract Test용 Stub 구현체
 * OpenAPI 스펙에 맞는 고정된 응답을 반환합니다.
 */
class StubPaymentService : PaymentOperations {

    private val idempotencyStorage = mutableMapOf<String, String>()

    override fun readyPayment(
        idempotencyKey: String,
        request: PaymentReadyRequest
    ): PaymentReadyResponse {
        verifyIdempotentRequest(idempotencyKey, request)

        return PaymentReadyResponse()
            .paymentKey("pay_${java.util.UUID.randomUUID()}")
            .checkoutUrl("https://payment.paybuddy.com/checkout/test")
            .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10))
    }

    private fun verifyIdempotentRequest(idempotencyKey: String, request: PaymentReadyRequest) {
        val currentPaymentRequestHash = "${request.merchantId}:${request.orderId}:${request.totalAmount}"

        val previousPaymentRequestHash = idempotencyStorage[idempotencyKey]
        if (previousPaymentRequestHash == null) {
            idempotencyStorage[idempotencyKey] = currentPaymentRequestHash
            return
        }

        if (currentPaymentRequestHash == previousPaymentRequestHash) {
            return
        }

        throw IdempotencyConflictException(idempotencyKey)
    }
}
