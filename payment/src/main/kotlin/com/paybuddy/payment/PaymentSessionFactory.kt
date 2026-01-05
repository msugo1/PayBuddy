package com.paybuddy.payment

import com.paybuddy.payment.domain.OrderLine
import com.paybuddy.payment.domain.PaymentAmount
import com.paybuddy.payment.domain.PaymentKeyGenerator
import com.paybuddy.payment.domain.PaymentPolicy
import com.paybuddy.payment.domain.PaymentSession
import com.paybuddy.payment.domain.RedirectUrl
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class PaymentSessionFactory(
    private val paymentKeyGenerator: PaymentKeyGenerator,
    private val paymentPolicy: PaymentPolicy
) {
    fun create(
        merchantId: String,
        orderId: String,
        orderLine: OrderLine,
        totalAmount: Long,
        supplyAmount: Long,
        vatAmount: Long,
        successUrl: String,
        failUrl: String
    ): PaymentSession {
        val id = paymentKeyGenerator.generate()
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
            id = id,
            merchantId = merchantId,
            orderId = orderId,
            orderLine = orderLine,
            amount = amount,
            expiresAt = expiresAt,
            redirectUrl = redirectUrl
        )
    }
}
