package com.paybuddy.payment.infrastructure.persistence

import com.paybuddy.payment.domain.Payment
import com.paybuddy.payment.domain.PaymentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class PaymentRepositoryAdapter(
    private val jpaPaymentRepository: JpaPaymentRepository
) : PaymentRepository {

    override fun save(payment: Payment): Payment {
        return jpaPaymentRepository.save(payment)
    }

    override fun findById(id: String): Payment? {
        return jpaPaymentRepository.findByIdOrNull(id)
    }

    override fun findByPaymentKey(paymentKey: String): Payment? {
        return jpaPaymentRepository.findByPaymentKey(paymentKey)
    }
}
