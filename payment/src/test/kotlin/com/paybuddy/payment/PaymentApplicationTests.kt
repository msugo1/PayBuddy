package com.paybuddy.payment

import com.paybuddy.payment.domain.FakePaymentSessionRepository
import com.paybuddy.payment.domain.PaymentSessionRepository
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
        // TODO: 현재 repository 는 인터페이스만 존재해서
        //  컨텍스트 띄우는 테스트는 실패한다.
        //  JPA + DB 붙인 후 제거
        @Bean
        fun paymentSessionRepository(): PaymentSessionRepository =
            FakePaymentSessionRepository()
    }

	@Test
	fun contextLoads() {
	}

}
