package com.paybuddy.payment

import com.paybuddy.payment.domain.PaymentKeyGenerator
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UuidPaymentKeyGenerator : PaymentKeyGenerator {
    override fun generate(): String = UUID.randomUUID().toString()
}