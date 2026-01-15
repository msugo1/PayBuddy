package com.paybuddy.payment.domain

interface PaymentSessionRepository {
    fun save(session: PaymentSession): PaymentSession

    // 만료되지 않은 session (expired = false) 조회를 대상으로 한다.
    fun findOngoingPaymentSession(merchantId: String, orderId: String): PaymentSession?
    fun findOngoingPaymentSession(paymentKey: String): PaymentSession?
}