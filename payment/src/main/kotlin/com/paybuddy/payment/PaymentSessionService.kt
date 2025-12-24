package com.paybuddy.payment

import com.paybuddy.payment.domain.OrderLine
import com.paybuddy.payment.domain.PaymentAmount
import com.paybuddy.payment.domain.PaymentKeyGenerator
import com.paybuddy.payment.domain.PaymentPolicy
import com.paybuddy.payment.domain.PaymentSession
import com.paybuddy.payment.domain.PaymentSessionConflictException
import com.paybuddy.payment.domain.PaymentSessionExpiredException
import com.paybuddy.payment.domain.PaymentSessionRepository
import com.paybuddy.payment.domain.RedirectUrl
import java.time.OffsetDateTime

class PaymentSessionService(
    private val paymentSessionRepository: PaymentSessionRepository,
    private val paymentKeyGenerator: PaymentKeyGenerator,
    private val checkoutBaseUrl: String,
) {
    fun prepare(
        merchantId: String,
        orderId: String,
        orderLine: OrderLine,
        amount: PaymentAmount,
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
                PaymentSession(
                    paymentKey = newPaymentKey,
                    merchantId = merchantId,
                    orderId = orderId,
                    orderLine = orderLine,
                    amount = amount,
                    expiresAt = OffsetDateTime.now().plusMinutes(PaymentPolicy.DEFAULT_EXPIRE_MINUTES),
                    redirectUrl = RedirectUrl(successUrl, failUrl),
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

        if (ongoingPaymentSession.isIdenticalPayment(merchantId, orderId, amount).not()) {
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