package com.paybuddy.payment

import com.paybuddy.payment.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@DisplayName("PaymentSessionService")
class PaymentSessionServiceTest {

    private lateinit var paymentSessionRepository: FakePaymentSessionRepository
    private lateinit var paymentKeyGenerator: PaymentKeyGenerator
    private lateinit var paymentSessionFactory: PaymentSessionFactory
    private lateinit var paymentSessionService: PaymentSessionService
    private lateinit var paymentPolicy: PaymentPolicy

    private val defaultCurrentTime = OffsetDateTime.now()
    private lateinit var expiresAt: OffsetDateTime

    @BeforeEach
    fun setUp() {
        paymentSessionRepository = FakePaymentSessionRepository()
        paymentKeyGenerator = FakePaymentKeyGenerator()
        paymentPolicy = DefaultPaymentPolicy()
        paymentSessionFactory = PaymentSessionFactory(paymentKeyGenerator, paymentPolicy)
        paymentSessionService = PaymentSessionService(
            paymentSessionRepository = paymentSessionRepository,
            paymentSessionFactory = paymentSessionFactory,
        )
        expiresAt = defaultCurrentTime
            .plusMinutes(paymentPolicy.defaultExpireMinutes)
    }

    @Test
    fun `진행중인 기존 결제세션이 없으면 새 결제세션을 준비한다`() {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"
        val orderLine = DEFAULT_ORDER_LINE
        val ongoingPaymentSession = paymentSessionRepository.findOngoingPaymentSession(merchantId, orderId)
        val paymentAmount = paymentPolicy.minPaymentAmount
        val successUrl = "https://success.com"
        val failUrl = "https://fail.com"

        // When
        val result = paymentSessionService.prepare(
            merchantId = merchantId,
            orderId = orderId,
            orderLine = orderLine,
            totalAmount = paymentAmount,
            supplyAmount = paymentAmount,
            vatAmount = 0,
            successUrl = successUrl,
            failUrl = failUrl
        )

        // Then
        assertThat(ongoingPaymentSession).isNull()

        assertThat(result.paymentKey).isEqualTo("pay_key_1")
        assertThat(result.merchantId).isEqualTo(merchantId)
        assertThat(result.orderId).isEqualTo(orderId)
        assertThat(result.orderLine).isEqualTo(orderLine)
        assertThat(result.amount).isEqualTo(PaymentAmount(paymentAmount, paymentAmount, 0))
        assertThat(result.expiresAt)
            .isAfter(defaultCurrentTime)
            .isBefore(defaultCurrentTime.plusMinutes(paymentPolicy.defaultExpireMinutes + 1))
        assertThat(result.redirectUrl).isEqualTo(RedirectUrl(successUrl, failUrl))
        assertThat(result.expired).isFalse()

        val savedSession = paymentSessionRepository.findByKey(merchantId, orderId)
        assertThat(savedSession).isNotNull
        assertThat(savedSession?.paymentKey).isEqualTo("pay_key_1")
    }

    @Test
    fun `만료예정 세션은 만료처리 후 예외를 일으켜 다시시작할 수 있게한다`() {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"
        val orderLine = DEFAULT_ORDER_LINE
        val amount = PaymentAmount(total = 10000, supply = 9091, vat = 909)

        val expiredPaymentSession = createPaymentSession(
            paymentKey = "pay_expired",
            merchantId = merchantId,
            orderId = orderId,
            orderLine = orderLine,
            amount = amount,
            expiresAt = defaultCurrentTime
        )

        paymentSessionRepository.save(expiredPaymentSession)

        // When & Then
        assertThatThrownBy {
            paymentSessionService.prepare(
                merchantId = merchantId,
                orderId = orderId,
                orderLine = orderLine,
                totalAmount = 10000,
                supplyAmount = 9091,
                vatAmount = 909,
                successUrl = "https://success.com",
                failUrl = "https://fail.com"
            )
        }.isInstanceOf(PaymentSessionExpiredException::class.java)

        // expired = true 확인
        val session = paymentSessionRepository.findByKey(merchantId, orderId)
        assertThat(session?.expired).isTrue()
    }

    @Test
    fun `이미 결제 세션이 생성된 주문건에 다른 결제 금액을 요청하면 예외가 발생한다`() {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"
        val orderLine = DEFAULT_ORDER_LINE
        val amount = PaymentAmount(total = 10000, supply = 9091, vat = 909)

        val existingSession = createPaymentSession(
            paymentKey = "pay_existing",
            merchantId = merchantId,
            orderId = orderId,
            orderLine = orderLine,
            amount = amount,
            expiresAt = expiresAt
        )
        paymentSessionRepository.save(existingSession)

        // When & Then - 다른 total
        assertThatThrownBy {
            paymentSessionService.prepare(
                merchantId = merchantId,
                orderId = orderId,
                orderLine = orderLine,
                totalAmount = 20000,
                supplyAmount = 18182,
                vatAmount = 1818,
                successUrl = "https://success.com",
                failUrl = "https://fail.com"
            )
        }.isInstanceOf(PaymentSessionConflictException::class.java)

        // When & Then - 같은 total이지만 다른 supply/vat
        assertThatThrownBy {
            paymentSessionService.prepare(
                merchantId = merchantId,
                orderId = orderId,
                orderLine = orderLine,
                totalAmount = 10000,
                supplyAmount = 9090,
                vatAmount = 910,
                successUrl = "https://success.com",
                failUrl = "https://fail.com"
            )
        }.isInstanceOf(PaymentSessionConflictException::class.java)
    }

    @Test
    fun `기존 진행중인 결제세션이 있고 동일한 정보로 요청했다면 기존 결제세션을 반환한다`() {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"
        val orderLine = DEFAULT_ORDER_LINE
        val paymentAmount = paymentPolicy.minPaymentAmount
        val amount = PaymentAmount(total = paymentAmount, supply = paymentAmount, vat = 0)
        val successUrl = "https://success.com"
        val failUrl = "https://fail.com"
        val redirectUrl = RedirectUrl(successUrl, failUrl)

        val existingSession = createPaymentSession(
            paymentKey = "pay_existing",
            merchantId = merchantId,
            orderId = orderId,
            orderLine = orderLine,
            amount = amount,
            expiresAt = expiresAt,
            redirectUrl = redirectUrl
        )
        paymentSessionRepository.save(existingSession)

        // When
        val result = paymentSessionService.prepare(
            merchantId = merchantId,
            orderId = orderId,
            orderLine = orderLine,
            totalAmount = paymentAmount,
            supplyAmount = paymentAmount,
            vatAmount = 0,
            successUrl = successUrl,
            failUrl = failUrl
        )

        // Then
        assertThat(result.paymentKey).isEqualTo("pay_existing")
        assertThat(result.merchantId).isEqualTo(merchantId)
        assertThat(result.orderId).isEqualTo(orderId)
        assertThat(result.orderLine).isEqualTo(orderLine)
        assertThat(result.amount).isEqualTo(amount)
        assertThat(result.expiresAt).isEqualTo(expiresAt)
        assertThat(result.redirectUrl).isEqualTo(redirectUrl)
        assertThat(result.expired).isFalse()
    }

    @Test
    fun `이전 결제세션이 만료되었다면 동일한 정보로 새 결제세션을 반환한다`() {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"
        val orderLine = DEFAULT_ORDER_LINE
        val amount = PaymentAmount(total = 10000, supply = 9091, vat = 909)
        val successUrl = "https://success.com"
        val failUrl = "https://fail.com"

        val expiredSession = createPaymentSession(
            paymentKey = "pay_old_expired",
            merchantId = merchantId,
            orderId = orderId,
            orderLine = orderLine,
            amount = amount,
            expiresAt = defaultCurrentTime
        ).apply {
            expire()
        }

        paymentSessionRepository.save(expiredSession)

        // When
        val result = paymentSessionService.prepare(
            merchantId = merchantId,
            orderId = orderId,
            orderLine = orderLine,
            totalAmount = 10000,
            supplyAmount = 9091,
            vatAmount = 909,
            successUrl = successUrl,
            failUrl = failUrl
        )

        // Then
        assertThat(result.paymentKey).isNotEqualTo("pay_old_expired")
        assertThat(result.paymentKey).isEqualTo("pay_key_1")
        assertThat(result.merchantId).isEqualTo(merchantId)
        assertThat(result.orderId).isEqualTo(orderId)
        assertThat(result.orderLine).isEqualTo(orderLine)
        assertThat(result.amount).isEqualTo(amount)
        assertThat(result.expiresAt)
            .isAfter(defaultCurrentTime)
            .isBefore(defaultCurrentTime.plusMinutes(paymentPolicy.defaultExpireMinutes + 1))
        assertThat(result.redirectUrl).isEqualTo(RedirectUrl(successUrl, failUrl))
        assertThat(result.expired).isFalse()

        val newSession = paymentSessionRepository.findByKey(merchantId, orderId)
        assertThat(newSession?.paymentKey).isEqualTo("pay_key_1")
    }

    @Test
    fun `최소 결제금액보다 적은 금액으로 결제 세션을 요청하면 결제세션 준비에 실패한다`() {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"
        val orderLine = DEFAULT_ORDER_LINE
        val paymentAmount = paymentPolicy.minPaymentAmount - 1

        // When & Then
        assertThatThrownBy {
            paymentSessionService.prepare(
                merchantId = merchantId,
                orderId = orderId,
                orderLine = orderLine,
                totalAmount = paymentAmount,
                supplyAmount = paymentAmount,
                vatAmount = 0,
                successUrl = "https://success.com",
                failUrl = "https://fail.com"
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun createPaymentSession(
        paymentKey: String,
        merchantId: String,
        orderId: String,
        orderLine: OrderLine,
        amount: PaymentAmount,
        expiresAt: OffsetDateTime,
        redirectUrl: RedirectUrl = RedirectUrl("https://success.com", "https://fail.com")
    ): PaymentSession {
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

/**
 * Fake PaymentKeyGenerator
 *
 * 순차적으로 증가하는 paymentKey 생성
 */
class FakePaymentKeyGenerator : PaymentKeyGenerator {
    private var counter = 0
    val generatedCount: Int
        get() = counter

    override fun generate(): String {
        return "pay_key_${++counter}"
    }
}
