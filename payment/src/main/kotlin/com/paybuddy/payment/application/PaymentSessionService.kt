package com.paybuddy.payment.application

import com.paybuddy.payment.PaymentSessionFactory
import com.paybuddy.payment.domain.*
import com.paybuddy.payment.service.PaymentSessionOperations
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class PaymentSessionService(
    private val paymentSessionRepository: PaymentSessionRepository,
    private val paymentSessionFactory: PaymentSessionFactory
) : PaymentSessionOperations {
    override fun prepare(
        merchantId: String,
        orderId: String,
        orderLine: OrderLine,
        totalAmount: Long,
        supplyAmount: Long,
        vatAmount: Long,
        successUrl: String,
        failUrl: String
    ): PaymentSession {
        val ongoingPaymentSession = paymentSessionRepository.findOngoingPaymentSession(
            merchantId = merchantId,
            orderId = orderId,
        )

        if (ongoingPaymentSession == null) {
            val newPaymentSession = paymentSessionRepository.save(
                paymentSessionFactory.create(
                    merchantId = merchantId,
                    orderId = orderId,
                    orderLine = orderLine,
                    totalAmount = totalAmount,
                    supplyAmount = supplyAmount,
                    vatAmount = vatAmount,
                    successUrl = successUrl,
                    failUrl = failUrl
                )
            )

            return newPaymentSession
        }

        if (ongoingPaymentSession.hasReachedExpiration(OffsetDateTime.now())) {
            ongoingPaymentSession.expire()
            paymentSessionRepository.save(ongoingPaymentSession)
            throw PaymentSessionExpiredException()
        }

        val requestedAmount = PaymentAmount(
            total = totalAmount,
            supply = supplyAmount,
            vat = vatAmount
        )

        if (ongoingPaymentSession.isIdenticalPayment(merchantId, orderId, requestedAmount).not()) {
            throw PaymentSessionConflictException()
        }

        return ongoingPaymentSession
    }

    override fun getOngoingSession(paymentKey: String): PaymentSession {
        val session = paymentSessionRepository.findOngoingPaymentSession(paymentKey)
            ?: throw PaymentSessionNotFoundException(paymentKey)

        if (session.hasReachedExpiration(OffsetDateTime.now())) {
            throw PaymentSessionExpiredException()
        }

        return session
    }
}
