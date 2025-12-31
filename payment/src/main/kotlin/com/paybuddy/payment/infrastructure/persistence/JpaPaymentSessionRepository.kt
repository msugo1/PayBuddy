package com.paybuddy.payment.infrastructure.persistence

import com.paybuddy.payment.domain.PaymentSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface JpaPaymentSessionRepository : JpaRepository<PaymentSession, String> {

    @Query("""
        SELECT ps FROM PaymentSession ps
        WHERE ps.merchantId = :merchantId
        AND ps.orderId = :orderId
        AND ps.expired = false
    """)
    fun findOngoingPaymentSession(
        @Param("merchantId") merchantId: String,
        @Param("orderId") orderId: String
    ): PaymentSession?
}
