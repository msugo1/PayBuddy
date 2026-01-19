package com.paybuddy.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

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
    @DisplayName("프로모션 적용")
    inner class AddEffectivePromotionsTest {

        @Test
        fun `결제 수단 정보가 없으면 예외를 던진다`() {
            // Given
            val payment = createPayment(originalAmount = 10000)
            val promotions = listOf(createPromotion(cardBrand = CardBrand.VISA))

            // When & Then
            assertThatThrownBy {
                payment.addEffectivePromotions(promotions, minPaymentAmount = 1000, KnapsackPromotionOptimizer)
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("결제 수단 정보가 설정되지 않았습니다")
        }

        @Test
        fun `매칭되는 프로모션이 없으면 적용되지 않는다`() {
            // Given
            val payment = createPayment(originalAmount = 10000)
            payment.submit(createCardDetails(brand = CardBrand.VISA))

            val promotions = listOf(
                createPromotion(cardBrand = CardBrand.MASTERCARD)  // 카드 브랜드 불일치
            )

            // When
            payment.addEffectivePromotions(promotions, minPaymentAmount = 1000, KnapsackPromotionOptimizer)

            // Then
            assertThat(payment.effectivePromotions).isEmpty()
            assertThat(payment.finalAmount).isEqualTo(10000)
        }

        @Test
        fun `모든 프로모션이 적용되면 정확히 변환되어 저장된다`() {
            // Given
            val payment = createPayment(originalAmount = 50000)
            payment.submit(createCardDetails(brand = CardBrand.VISA, cardType = CardType.CREDIT))

            val promotions = listOf(
                createPromotion(
                    id = "promo1",
                    name = "FIXED 프로모션",
                    provider = PromotionProvider.CARD_ISSUER,
                    discountType = DiscountType.FIXED,
                    discountValue = 3000,
                    cardBrand = CardBrand.VISA
                ),
                createPromotion(
                    id = "promo2",
                    name = "PERCENTAGE 프로모션",
                    provider = PromotionProvider.PLATFORM,
                    discountType = DiscountType.PERCENTAGE,
                    discountValue = 10,  // 10%
                    maxDiscountAmount = 4000,
                    cardBrand = CardBrand.VISA
                )
            )

            // When
            payment.addEffectivePromotions(promotions, minPaymentAmount = 1000, KnapsackPromotionOptimizer)

            // Then
            assertThat(payment.effectivePromotions).hasSize(2)

            val fixedPromotion = payment.effectivePromotions.find { it.name == "FIXED 프로모션" }
            assertThat(fixedPromotion).isNotNull
            assertThat(fixedPromotion!!.provider).isEqualTo(PromotionProvider.CARD_ISSUER)
            assertThat(fixedPromotion.amount).isEqualTo(3000)

            val percentagePromotion = payment.effectivePromotions.find { it.name == "PERCENTAGE 프로모션" }
            assertThat(percentagePromotion).isNotNull
            assertThat(percentagePromotion!!.provider).isEqualTo(PromotionProvider.PLATFORM)
            assertThat(percentagePromotion.amount).isEqualTo(4000)  // min(50000 * 10%, 4000) = 4000

            assertThat(payment.finalAmount).isEqualTo(43000)  // 50000 - 3000 - 4000
        }

        @Test
        fun `일부 프로모션만 적용되면 선택된 것만 저장된다`() {
            // Given
            val payment = createPayment(originalAmount = 10000)
            payment.submit(createCardDetails(brand = CardBrand.VISA))

            val promotions = listOf(
                createPromotion(
                    id = "promo1",
                    name = "선택될 프로모션",
                    discountType = DiscountType.FIXED,
                    discountValue = 3000,
                    cardBrand = CardBrand.VISA
                ),
                createPromotion(
                    id = "promo2",
                    name = "제외될 프로모션",
                    discountType = DiscountType.FIXED,
                    discountValue = 7000,  // maxDiscountLimit 초과
                    cardBrand = CardBrand.VISA
                )
            )

            // When (maxDiscountLimit = 10000 - 1000 = 9000, promo2는 7000이라 선택 가능하지만 promo1만 선택됨)
            payment.addEffectivePromotions(promotions, minPaymentAmount = 1000, KnapsackPromotionOptimizer)

            // Then
            assertThat(payment.effectivePromotions).hasSize(1)
            assertThat(payment.effectivePromotions[0].name).isEqualTo("제외될 프로모션")
            assertThat(payment.effectivePromotions[0].amount).isEqualTo(7000)
            assertThat(payment.finalAmount).isEqualTo(3000)  // 10000 - 7000
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
            assertThat(payment.paymentResult?.errorCode).isEqualTo("VALIDATION_ERROR")
            assertThat(payment.paymentResult?.failureReason).isEqualTo("카드 정보 검증 실패")
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
        paymentMethodType: PaymentMethodType = PaymentMethodType.CARD,
        status: PaymentStatus = PaymentStatus.INITIALIZED,
        originalAmount: Long = 10000
    ): Payment {
        return Payment(
            id = id,
            paymentKey = paymentKey,
            merchantId = merchantId,
            paymentMethodType = paymentMethodType,
            status = status,
            originalAmount = originalAmount
        )
    }

    private fun createCardDetails(
        maskedNumber: String = "1234-56**-****-7890",
        expiryMonth: Int? = 12,
        expiryYear: Int? = 25,
        holderName: String? = null,
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
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                holderName = holderName,
                bin = bin,
                brand = brand,
                issuerCode = issuerCode,
                acquirerCode = acquirerCode,
                cardType = cardType,
                ownerType = ownerType,
                issuedCountry = issuedCountry,
                productCode = productCode
            ),
            installment = null
        )
    }

    private fun createPaymentWithCard(
        id: String = "01JGXM9K3V7N2P8Q4R5S6T7U8V",
        paymentKey: String = "01JGXM9K3V7N2P8Q4R5S6T7U9W",
        merchantId: String = "mch_123",
        paymentMethodType: PaymentMethodType = PaymentMethodType.CARD,
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
            paymentMethodType = paymentMethodType,
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

    private fun createPromotion(
        id: String = "promo_123",
        name: String = "테스트 프로모션",
        provider: PromotionProvider = PromotionProvider.PLATFORM,
        discountType: DiscountType = DiscountType.FIXED,
        discountValue: Long = 1000,
        maxDiscountAmount: Long? = null,
        cardBrand: CardBrand? = null,
        cardType: CardType? = null,
        issuerCode: String? = null,
        minAmount: Long? = null,
        validFrom: Instant = Instant.now(),
        validUntil: Instant = Instant.now().plusSeconds(86400)
    ): Promotion {
        return Promotion(
            id = id,
            name = name,
            provider = provider,
            discountType = discountType,
            discountValue = discountValue,
            maxDiscountAmount = maxDiscountAmount,
            cardBrand = cardBrand,
            cardType = cardType,
            issuerCode = issuerCode,
            minAmount = minAmount,
            validFrom = validFrom,
            validUntil = validUntil
        )
    }
}
