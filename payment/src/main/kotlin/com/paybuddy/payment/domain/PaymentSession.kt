package com.paybuddy.payment.domain

import java.time.OffsetDateTime

/**
 * 결제 세션
 *
 * 결제 준비 시 생성되며 expireAt까지 유효하다.
 * merchantId + orderId 조합은 고유해야 한다.
 */
data class PaymentSession(
    val id: Long = 0,
    val paymentKey: String,
    val merchantId: String,
    val orderId: String,
    val orderLine: OrderLine,
    val amount: PaymentAmount,
    val expireAt: OffsetDateTime,
    val successUrl: String,
    val failUrl: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) {
    fun isExpired(): Boolean = OffsetDateTime.now().isAfter(expireAt)
}
