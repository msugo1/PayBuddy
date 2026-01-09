package com.paybuddy.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PaymentTest {

    @Nested
    @DisplayName("finalAmount 계산")
    inner class FinalAmountTest {

        @Test
        fun `프로모션이 없으면 originalAmount와 동일하다`() {
            // Given
            val payment = createPayment(originalAmount = 10000)

            // When
            val finalAmount = payment.finalAmount

            // Then
            assertThat(finalAmount).isEqualTo(10000)
        }
    }

    @Nested
    @DisplayName("결제 실패 처리")
    inner class FailTest {

        @Test
        fun `결제 수단 정보가 설정되어 있으면 실패 처리가 가능하다`() {
            // Given
            val payment = createPayment(status = PaymentStatus.INITIALIZED)
            val cardDetails = createCardDetails()
            payment.submit(cardDetails)

            // When
            payment.fail("VALIDATION_ERROR", "카드 정보 검증 실패")

            // Then
            assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
            assertThat(payment.cardPaymentDetails?.result?.errorCode).isEqualTo("VALIDATION_ERROR")
            assertThat(payment.cardPaymentDetails?.result?.failureReason).isEqualTo("카드 정보 검증 실패")
        }

        @Test
        fun `결제 수단을 제출하면 카드 정보가 설정된다`() {
            // Given
            val payment = createPayment(status = PaymentStatus.INITIALIZED)
            val cardDetails = createCardDetails()

            // When
            payment.submit(cardDetails)

            // Then
            assertThat(payment.cardPaymentDetails).isEqualTo(cardDetails)
        }

        @Test
        fun `결제 수단 정보가 없으면 실패 처리할 수 없다`() {
            // Given
            val payment = createPayment(status = PaymentStatus.INITIALIZED)

            // When & Then
            assertThatThrownBy {
                payment.fail("ERROR", "실패 처리 시도")
            }.isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun `이미 결제 수단이 제출되었으면 중복 제출할 수 없다`() {
            // Given
            val payment = createPayment(status = PaymentStatus.INITIALIZED)
            payment.submit(createCardDetails())

            // When & Then
            assertThatThrownBy {
                payment.submit(createCardDetails())
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("인증 처리")
    inner class AuthenticationTest {

        @Test
        fun `결제 수단 정보가 없으면 인증을 요청할 수 없다`() {
            // Given
            val payment = createPayment(status = PaymentStatus.INITIALIZED)

            // When & Then
            assertThatThrownBy {
                payment.requestAuthentication()
            }.isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun `결제 수단 정보가 없으면 인증을 완료할 수 없다`() {
            // Given
            val payment = createPayment(status = PaymentStatus.INITIALIZED)

            // When & Then
            assertThatThrownBy {
                payment.completeAuthentication()
            }.isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun `결제 수단 정보가 없으면 submit 완료할 수 없다`() {
            // Given
            val payment = createPayment(status = PaymentStatus.INITIALIZED)

            // When & Then
            assertThatThrownBy {
                payment.completeWithoutAuthentication()
            }.isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun `인증 완료는 AUTHENTICATION_REQUIRED 상태에서만 가능하다`() {
            // Given
            val payment = createPayment(status = PaymentStatus.INITIALIZED)
            payment.submit(createCardDetails())

            // When & Then
            assertThatThrownBy {
                payment.completeAuthentication()
            }.isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        fun `인증 없이 진행은 INITIALIZED 상태에서만 가능하다`() {
            // Given
            val payment = createPayment(status = PaymentStatus.INITIALIZED)
            payment.submit(createCardDetails())
            payment.requestAuthentication()

            // When & Then
            assertThatThrownBy {
                payment.completeWithoutAuthentication()
            }.isInstanceOf(IllegalStateException::class.java)
        }
    }

    private fun createPayment(
        id: String = "01JGXM9K3V7N2P8Q4R5S6T7U8V",
        paymentKey: String = "01JGXM9K3V7N2P8Q4R5S6T7U9W",
        merchantId: String = "mch_123",
        status: PaymentStatus = PaymentStatus.INITIALIZED,
        originalAmount: Long = 10000
    ): Payment {
        return Payment(
            id = id,
            paymentKey = paymentKey,
            merchantId = merchantId,
            status = status,
            originalAmount = originalAmount
        )
    }

    private fun createCardDetails(
        maskedNumber: String = "1234-56**-****-7890",
        bin: String = "123456",
        brand: CardBrand? = CardBrand.VISA,
        issuerCode: String = "04",
        acquirerCode: String = "04",
        cardType: CardType = CardType.CREDIT,
        ownerType: OwnerType = OwnerType.PERSONAL,
        issuedCountry: String = "KR",
        productCode: String? = null
    ): CardPaymentDetails {
        return CardPaymentDetails(
            card = Card(
                maskedNumber = maskedNumber,
                bin = bin,
                brand = brand,
                issuerCode = issuerCode,
                acquirerCode = acquirerCode,
                cardType = cardType,
                ownerType = ownerType,
                issuedCountry = issuedCountry,
                productCode = productCode
            ),
            installmentMonths = 0
        )
    }

    private fun createPaymentWithCard(
        id: String = "01JGXM9K3V7N2P8Q4R5S6T7U8V",
        paymentKey: String = "01JGXM9K3V7N2P8Q4R5S6T7U9W",
        merchantId: String = "mch_123",
        status: PaymentStatus = PaymentStatus.INITIALIZED,
        originalAmount: Long = 10000,
        cardBrand: CardBrand? = CardBrand.VISA,
        cardType: CardType = CardType.CREDIT,
        issuerCode: String = "04"
    ): Payment {
        val payment = Payment(
            id = id,
            paymentKey = paymentKey,
            merchantId = merchantId,
            status = status,
            originalAmount = originalAmount
        )
        payment.submit(
            createCardDetails(
                brand = cardBrand,
                cardType = cardType,
                issuerCode = issuerCode
            )
        )
        return payment
    }
}
