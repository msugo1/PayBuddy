package com.paybuddy.payment.domain

import java.time.OffsetDateTime

data class PreparedPaymentSession(
    val paymentKey: String,
    val checkoutUrl: String,
    val expiresAt: OffsetDateTime
)