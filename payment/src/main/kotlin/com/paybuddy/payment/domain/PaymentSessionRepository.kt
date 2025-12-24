package com.paybuddy.payment.domain

interface PaymentSessionRepository {
    fun save(session: PaymentSession): PaymentSession
    fun findByPaymentKey(paymentKey: String): PaymentSession?
}