package com.paybuddy.payment.infrastructure.persistence

import com.paybuddy.payment.domain.PaymentSession
import com.paybuddy.payment.domain.PaymentSessionRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class PaymentSessionRepositoryAdapter(
    private val jpaPaymentSessionRepository: JpaPaymentSessionRepository
) : PaymentSessionRepository {

    override fun save(session: PaymentSession): PaymentSession {
        return jpaPaymentSessionRepository.save(session)
    }

    override fun findOngoingPaymentSession(merchantId: String, orderId: String): PaymentSession? {
        return jpaPaymentSessionRepository.findByMerchantIdAndOrderIdAndExpiredFalse(merchantId, orderId)
    }

    override fun findOngoingPaymentSession(paymentKey: String): PaymentSession? {
        return jpaPaymentSessionRepository.findByIdOrNull(paymentKey)
    }
}
