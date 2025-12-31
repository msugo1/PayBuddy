package com.paybuddy.payment.infrastructure.persistence

import com.paybuddy.payment.domain.PaymentSession
import org.springframework.data.jpa.repository.JpaRepository

interface JpaPaymentSessionRepository : JpaRepository<PaymentSession, String> {

    fun findByMerchantIdAndOrderIdAndExpiredFalse(
        merchantId: String,
        orderId: String
    ): PaymentSession?
}
