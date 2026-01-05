package com.paybuddy.payment.config

import com.paybuddy.payment.service.PaymentOperations
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class PaymentTestConfig {

    @Bean
    @Primary
    fun paymentOperations(): PaymentOperations {
        return StubPaymentService()
    }
}
