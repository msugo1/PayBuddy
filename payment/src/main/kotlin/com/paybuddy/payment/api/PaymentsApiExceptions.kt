package com.paybuddy.payment.api

class IdempotencyConflictException(
    val idempotencyKey: String,
    message: String = "Idempotency-Key reused with a different request payload"
) : RuntimeException(message)