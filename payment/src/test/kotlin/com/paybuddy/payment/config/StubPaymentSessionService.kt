package com.paybuddy.payment.config

import com.github.f4b6a3.ulid.Ulid
import com.paybuddy.payment.domain.OrderLine
import com.paybuddy.payment.domain.PaymentAmount
import com.paybuddy.payment.domain.PaymentSession
import com.paybuddy.payment.domain.RedirectUrl
import com.paybuddy.payment.service.PaymentSessionOperations
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Contract Test용 Stub 구현체
 * OpenAPI 스펙에 맞는 고정된 응답을 반환합니다.
 */
class StubPaymentSessionService : PaymentSessionOperations {

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
        return PaymentSession(
            id = Ulid.fast().toString(),
            merchantId = merchantId,
            orderId = orderId,
            orderLine = orderLine,
            amount = PaymentAmount(
                total = totalAmount,
                supply = supplyAmount,
                vat = vatAmount
            ),
            redirectUrl = RedirectUrl(
                success = successUrl,
                fail = failUrl
            ),
            expiresAt = OffsetDateTime.now(ZoneOffset.UTC)
        )
    }

    override fun getOngoingSession(paymentKey: String): PaymentSession {
        error("Not implemented for contract test")
    }
}
