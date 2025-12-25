package com.paybuddy.payment

import com.paybuddy.payment.domain.OrderLine
import com.paybuddy.payment.domain.PaymentAmount
import com.paybuddy.payment.domain.PaymentKeyGenerator
import com.paybuddy.payment.domain.PaymentSession
import com.paybuddy.payment.domain.PaymentSessionConflictException
import com.paybuddy.payment.domain.PaymentSessionExpiredException
import com.paybuddy.payment.domain.PaymentSessionRepository
import java.time.OffsetDateTime

class PaymentSessionService(
    private val paymentSessionRepository: PaymentSessionRepository,
    private val paymentKeyGenerator: PaymentKeyGenerator,
    private val paymentSessionFactory: PaymentSessionFactory,
) {
    fun prepare(
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
            val newPaymentKey = paymentKeyGenerator.generate()

            val newPaymentSession = paymentSessionRepository.save(
                paymentSessionFactory.create(
                    paymentKey = newPaymentKey,
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
}