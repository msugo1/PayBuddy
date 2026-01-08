package com.paybuddy.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

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

        @Test
        fun `프로모션이 있으면 할인 금액만큼 차감된다`() {
            // Given
            val payment = createPayment(originalAmount = 10000)
            payment.addPromotion(
                EffectivePromotion(
                    name = "신규 회원 할인",
                    provider = PromotionProvider.PLATFORM,
                    amount = 1000
                ),
                minPaymentAmount = 1000
            )

            // When
            val finalAmount = payment.finalAmount

            // Then
            assertThat(finalAmount).isEqualTo(9000)
        }

        @Test
        fun `여러 프로모션이 있으면 모든 할인 금액이 차감된다`() {
            // Given
            val payment = createPayment(originalAmount = 10000)
            payment.addPromotion(
                EffectivePromotion(
                    name = "플랫폼 할인",
                    provider = PromotionProvider.PLATFORM,
                    amount = 1000
                ),
                minPaymentAmount = 1000
            )
            payment.addPromotion(
                EffectivePromotion(
                    name = "카드사 할인",
                    provider = PromotionProvider.CARD_ISSUER,
                    amount = 500
                ),
                minPaymentAmount = 1000
            )

            // When
            val finalAmount = payment.finalAmount

            // Then
            assertThat(finalAmount).isEqualTo(8500)
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
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("결제 수단 정보가 설정되지 않았습니다")
        }
    }

    @Nested
    @DisplayName("프로모션 적용")
    inner class AddPromotionTest {

        @ParameterizedTest(name = "원금 {0}원에 할인 {1}원 적용 → 최종 {2}원")
        @CsvSource(
            "10000, 1000, 9000",
            "10000, 5000, 5000",
            "10000, 9000, 1000"
        )
        fun `할인 금액이 유효하면 프로모션이 적용된다`(originalAmount: Long, promotionAmount: Long, expectedFinal: Long) {
            // Given
            val payment = createPayment(originalAmount = originalAmount)

            // When
            payment.addPromotion(
                EffectivePromotion(
                    name = "테스트 할인",
                    provider = PromotionProvider.PLATFORM,
                    amount = promotionAmount
                ),
                minPaymentAmount = 1000
            )

            // Then
            assertThat(payment.finalAmount).isEqualTo(expectedFinal)
        }

        @ParameterizedTest(name = "원금 {0}원에 할인 {1}원 적용 시 예외")
        @CsvSource(
            "10000, 0",
            "10000, -100",
            "10000, 9001",
            "10000, 10000"
        )
        fun `할인 금액이 0 이하이거나 최종 금액이 최소 금액 미만이 되면 예외가 발생한다`(originalAmount: Long, promotionAmount: Long) {
            // Given
            val payment = createPayment(originalAmount = originalAmount)

            // When & Then
            assertThatThrownBy {
                payment.addPromotion(
                    EffectivePromotion(
                        name = "잘못된 할인",
                        provider = PromotionProvider.PLATFORM,
                        amount = promotionAmount
                    ),
                    minPaymentAmount = 1000
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
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

    private fun createCardDetails(): CardPaymentDetails {
        return CardPaymentDetails(
            card = Card(
                maskedNumber = "1234********7890",
                expiryMonth = 12,
                expiryYear = 25,
                holderName = null,
                bin = "123456",
                brand = CardBrand.VISA,
                issuerCode = "04",
                acquirerCode = "04",
                cardType = CardType.CREDIT,
                ownerType = OwnerType.PERSONAL,
                issuedCountry = "KR",
                productCode = null
            ),
            installment = null
        )
    }
}
