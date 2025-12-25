package com.paybuddy.payment

import com.paybuddy.payment.domain.PaymentKeyGenerator
import java.util.UUID

class UuidPaymentKeyGenerator : PaymentKeyGenerator {
    override fun generate(): String = UUID.randomUUID().toString()
}