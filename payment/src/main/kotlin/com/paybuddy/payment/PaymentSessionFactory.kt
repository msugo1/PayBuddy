package com.paybuddy.payment

import com.paybuddy.payment.domain.OrderLine
import com.paybuddy.payment.domain.PaymentAmount
import com.paybuddy.payment.domain.PaymentPolicy
import com.paybuddy.payment.domain.PaymentSession
import com.paybuddy.payment.domain.RedirectUrl
import java.time.OffsetDateTime

/**
 * PaymentSession 생성 팩토리
 *
 * 정책 검증 및 만료시간 계산을 담당
 */
class PaymentSessionFactory(
    private val paymentPolicy: PaymentPolicy
) {
    fun create(
        paymentKey: String,
        merchantId: String,
        orderId: String,
        orderLine: OrderLine,
        totalAmount: Long,
        supplyAmount: Long,
        vatAmount: Long,
        successUrl: String,
        failUrl: String
    ): PaymentSession {
        require(totalAmount >= paymentPolicy.minPaymentAmount) {
            "Payment amount must be at least ${paymentPolicy.minPaymentAmount}"
        }

        val amount = PaymentAmount(
            total = totalAmount,
            supply = supplyAmount,
            vat = vatAmount
        )
        val expiresAt = OffsetDateTime.now().plusMinutes(paymentPolicy.defaultExpireMinutes)
        val redirectUrl = RedirectUrl(success = successUrl, fail = failUrl)

        return PaymentSession(
            paymentKey = paymentKey,
            merchantId = merchantId,
            orderId = orderId,
            orderLine = orderLine,
            amount = amount,
            expiresAt = expiresAt,
            redirectUrl = redirectUrl
        )
    }
}
