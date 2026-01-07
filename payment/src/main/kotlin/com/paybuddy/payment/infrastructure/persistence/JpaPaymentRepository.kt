package com.paybuddy.payment.infrastructure.persistence

import com.paybuddy.payment.domain.Payment
import org.springframework.data.jpa.repository.JpaRepository

interface JpaPaymentRepository : JpaRepository<Payment, String> {
    fun findByPaymentKey(paymentKey: String): Payment?
}
