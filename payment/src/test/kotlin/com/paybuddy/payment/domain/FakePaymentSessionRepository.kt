package com.paybuddy.payment.domain

class FakePaymentSessionRepository : PaymentSessionRepository {
    private val sessions = mutableMapOf<Pair<String, String>, PaymentSession>()
    private var idCounter = 1L

    override fun save(session: PaymentSession): PaymentSession {
        val key = session.merchantId to session.orderId
        sessions[key] = session
        return session
    }

    override fun findOngoingPaymentSession(merchantId: String, orderId: String): PaymentSession? {
        val key = merchantId to orderId
        return sessions[key]?.takeIf { !it.expired }
    }

    override fun findOngoingPaymentSession(paymentKey: String): PaymentSession? {
        return sessions.values.firstOrNull { it.id == paymentKey && !it.expired }
    }

    fun findByKey(merchantId: String, orderId: String): PaymentSession? {
        return sessions[merchantId to orderId]
    }
}
