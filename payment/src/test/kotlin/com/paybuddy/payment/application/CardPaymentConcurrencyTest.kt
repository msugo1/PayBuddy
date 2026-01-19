package com.paybuddy.payment.application

import com.paybuddy.payment.PaymentApplicationTests
import com.paybuddy.payment.application.dto.SubmitCardPaymentCommand
import com.paybuddy.payment.application.dto.SubmitPaymentResult
import com.paybuddy.payment.domain.*
import com.paybuddy.payment.infrastructure.persistence.JpaPaymentRepository
import com.paybuddy.payment.infrastructure.persistence.JpaPaymentSessionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@ActiveProfiles("test")
@Import(PaymentApplicationTests.TestConfig::class)
@DisplayName("CardPaymentUseCase 동시성 테스트")
class CardPaymentConcurrencyTest {

    @Autowired
    private lateinit var cardPaymentUseCase: CardPaymentUseCase

    @Autowired
    private lateinit var paymentRepository: JpaPaymentRepository

    @Autowired
    private lateinit var paymentSessionRepository: JpaPaymentSessionRepository

    @BeforeEach
    fun setUp() {
        paymentRepository.deleteAll()
        paymentSessionRepository.deleteAll()
    }

    @ParameterizedTest
    @ValueSource(ints = [2, 3])
    fun `동일한 결제에 대한 중복 요청은 하나만 처리된다`(threadCount: Int) {
        // Given
        val session = createPaymentSession()
        paymentSessionRepository.save(session)
        val request = createSubmitRequest(paymentKey = session.id)

        val executorService = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val results = ConcurrentLinkedQueue<Result<SubmitPaymentResult>>()

        // When
        repeat(threadCount) {
            executorService.submit {
                try {
                    val response = cardPaymentUseCase.submit(request)
                    results.add(Result.success(response))
                } catch (e: Exception) {
                    results.add(Result.failure(e))
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        executorService.shutdown()

        // Then
        val successes = results.count { it.isSuccess }
        val failures = results.count { it.isFailure }

        assertThat(successes).isEqualTo(1)
        assertThat(failures).isEqualTo(threadCount - 1)

        val payments = paymentRepository.findAll()
        assertThat(payments).hasSize(1)
    }

    private fun createPaymentSession(): PaymentSession {
        return PaymentSession(
            id = "pay_${System.nanoTime()}",
            merchantId = "mch_123",
            orderId = "order_456",
            orderLine = OrderLine(
                listOf(
                    OrderLineItem(
                        name = "테스트상품",
                        quantity = 1,
                        unitAmount = 10_000,
                        imageUrl = "https://example.com/product.jpg"
                    )
                )
            ),
            amount = PaymentAmount(10_000, 10_000, 0),
            expiresAt = OffsetDateTime.now().plusMinutes(15),
            redirectUrl = RedirectUrl("https://success.com", "https://fail.com")
        )
    }

    private fun createSubmitRequest(paymentKey: String): SubmitCardPaymentCommand {
        return SubmitCardPaymentCommand(
            paymentKey = paymentKey,
            cardNumber = "4532015112830366",
            expiryMonth = 12,
            expiryYear = 30,
            holderName = "홍길동",
            cvc = "123",
            installmentMonths = 0
        )
    }
}
