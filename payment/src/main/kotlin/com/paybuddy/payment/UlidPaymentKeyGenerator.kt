package com.paybuddy.payment

import com.github.f4b6a3.ulid.UlidCreator
import com.paybuddy.payment.domain.PaymentKeyGenerator
import org.springframework.stereotype.Component

@Component
class UlidPaymentKeyGenerator : PaymentKeyGenerator {
    override fun generate(): String {
        return UlidCreator.getMonotonicUlid().toString()
    }
}
