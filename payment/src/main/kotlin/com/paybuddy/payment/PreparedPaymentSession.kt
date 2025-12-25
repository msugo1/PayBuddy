package com.paybuddy.payment

import java.time.OffsetDateTime

/**
 * 결제 준비 완료 응답
 */
data class PreparedPaymentSession(
    val paymentKey: String,
    val checkoutUrl: String,
    val expiresAt: OffsetDateTime
)