package com.paybuddy.payment

import com.paybuddy.payment.domain.OrderLine
import com.paybuddy.payment.domain.PaymentAmount
import com.paybuddy.payment.domain.PaymentKeyGenerator
import com.paybuddy.payment.domain.PaymentSessionConflictException
import com.paybuddy.payment.domain.PaymentSessionExpiredException
import com.paybuddy.payment.domain.PaymentSessionRepository

class PaymentSessionService(
    private val paymentSessionRepository: PaymentSessionRepository,
    private val paymentKeyGenerator: PaymentKeyGenerator,
    private val paymentSessionFactory: PaymentSessionFactory,
    private val checkoutBaseUrl: String,
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
    ): PreparedPaymentSession {
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

            return PreparedPaymentSession(
                paymentKey = newPaymentKey,
                checkoutUrl = buildCheckoutUrl(newPaymentSession.paymentKey),
                expiresAt = newPaymentSession.expiresAt,
            )
        }

        if (ongoingPaymentSession.hasReachedExpiration()) {
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

        return PreparedPaymentSession(
            paymentKey = ongoingPaymentSession.paymentKey,
            checkoutUrl = buildCheckoutUrl(ongoingPaymentSession.paymentKey),
            expiresAt = ongoingPaymentSession.expiresAt
        )
    }

    private fun buildCheckoutUrl(paymentKey: String): String {
        return "$checkoutBaseUrl/checkout?key=$paymentKey"
    }
}