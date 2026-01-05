package com.paybuddy.payment.service

import com.paybuddy.payment.api.IdempotencyConflictException
import com.paybuddy.payment.api.model.PaymentReadyRequest
import com.paybuddy.payment.api.model.PaymentReadyResponse
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class PaymentService : PaymentOperations {

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
        // FIXME: 일단 제대로 구현하기 전까지는 임의로 hash 되었다고 간주한다.
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
