package com.paybuddy.payment

import com.paybuddy.payment.domain.DefaultPaymentPolicy
import com.paybuddy.payment.domain.PaymentAmount
import com.paybuddy.payment.domain.PaymentKeyGenerator
import com.paybuddy.payment.domain.PaymentPolicy
import com.paybuddy.payment.domain.RedirectUrl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@DisplayName("PaymentSessionFactory")
class PaymentSessionFactoryTest {

    private lateinit var paymentKeyGenerator: PaymentKeyGenerator
    private lateinit var paymentPolicy: PaymentPolicy
    private lateinit var paymentSessionFactory: PaymentSessionFactory

    @BeforeEach
    fun setUp() {
        paymentKeyGenerator = UuidPaymentKeyGenerator()
        paymentPolicy = DefaultPaymentPolicy()
        paymentSessionFactory = PaymentSessionFactory(paymentKeyGenerator, paymentPolicy)
    }

    @ParameterizedTest
    @ValueSource(longs = [1000, 1001, 10000, 50000])
    @DisplayName("최소 결제 금액 이상이면 결제세션이 생성된다")
    fun `최소 결제 금액 정책을 만족하면 세션이 생성된다`(totalAmount: Long) {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"
        val orderLine = DEFAULT_ORDER_LINE
        val supplyAmount = (totalAmount * 0.9091).toLong()
        val vatAmount = totalAmount - supplyAmount

        // When
        val session = paymentSessionFactory.create(
            merchantId = merchantId,
            orderId = orderId,
            orderLine = orderLine,
            totalAmount = totalAmount,
            supplyAmount = supplyAmount,
            vatAmount = vatAmount,
            successUrl = "https://success.com",
            failUrl = "https://fail.com"
        )

        // Then
        assertThat(session.id).hasSize(26)  // ULID는 26자
        assertThat(session.merchantId).isEqualTo(merchantId)
        assertThat(session.orderId).isEqualTo(orderId)
        assertThat(session.orderLine).isEqualTo(orderLine)
        assertThat(session.amount).isEqualTo(PaymentAmount(
            total = totalAmount,
            supply = supplyAmount,
            vat = vatAmount
        ))
        assertThat(session.redirectUrl).isEqualTo(RedirectUrl(
            success = "https://success.com",
            fail = "https://fail.com"
        ))
        assertThat(session.expired).isFalse()
    }

    @ParameterizedTest
    @ValueSource(longs = [0, 500, 999])
    @DisplayName("최소 결제 금액 미만이면 예외가 발생한다")
    fun `최소 결제 금액 정책을 위반하면 세션 생성이 거부된다`(totalAmount: Long) {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"
        val orderLine = DEFAULT_ORDER_LINE
        val supplyAmount = (totalAmount * 0.9091).toLong()
        val vatAmount = totalAmount - supplyAmount

        // When & Then
        assertThatThrownBy {
            paymentSessionFactory.create(
                merchantId = merchantId,
                orderId = orderId,
                orderLine = orderLine,
                totalAmount = totalAmount,
                supplyAmount = supplyAmount,
                vatAmount = vatAmount,
                successUrl = "https://success.com",
                failUrl = "https://fail.com"
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("새 결제세션의 만료시간은 정책의 기본 만료시간으로 설정된다")
    fun `생성된 세션의 만료시간은 정책 기본값을 따른다`() {
        // Given
        val merchantId = "mch_123"
        val orderId = "order_456"
        val orderLine = DEFAULT_ORDER_LINE
        val currentTime = OffsetDateTime.now()

        // When
        val session = paymentSessionFactory.create(
            merchantId = merchantId,
            orderId = orderId,
            orderLine = orderLine,
            totalAmount = 10000,
            supplyAmount = 9091,
            vatAmount = 909,
            successUrl = "https://success.com",
            failUrl = "https://fail.com"
        )

        // Then
        val expectedExpiry = currentTime.plusMinutes(paymentPolicy.defaultExpireMinutes)
        assertThat(session.expiresAt)
            .isCloseTo(expectedExpiry, within(1, ChronoUnit.SECONDS))
    }

}
