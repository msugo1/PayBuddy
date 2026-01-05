package com.paybuddy.payment.api

import org.springframework.stereotype.Component

@Component
class IdempotencyValidator {
    private val storage = mutableMapOf<String, String>()

    fun validate(idempotencyKey: String, requestHash: String) {
        val previousHash = storage[idempotencyKey]

        if (previousHash == null) {
            storage[idempotencyKey] = requestHash
            return
        }

        if (requestHash != previousHash) {
            throw IdempotencyConflictException(idempotencyKey)
        }
    }

    fun hashRequest(merchantId: String, orderId: String, totalAmount: Long): String {
        return "$merchantId:$orderId:$totalAmount"
    }
}
