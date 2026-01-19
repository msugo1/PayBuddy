package com.paybuddy.payment

import com.paybuddy.payment.config.StubBinLookupService
import com.paybuddy.payment.config.StubCardVaultService
import com.paybuddy.payment.domain.service.BinLookupService
import com.paybuddy.payment.domain.service.CardVaultService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles


@SpringBootTest
@ActiveProfiles("test")
class PaymentApplicationTests {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun binLookupService(): BinLookupService =
            StubBinLookupService()

        @Bean
        fun cardVaultService(): CardVaultService =
            StubCardVaultService()
    }

	@Test
	fun contextLoads() {
	}

}
