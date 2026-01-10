package com.paybuddy.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant

class PromotionTest {

    @Nested
    @DisplayName("생성 검증")
    inner class ValidationTest {

        @Test
        fun `FIXED 타입에서 discountValue가 maxDiscountAmount보다 크면 예외`() {
            // When & Then
            assertThatThrownBy {
                createPromotion(
                    discountType = DiscountType.FIXED,
                    discountValue = 7000,
                    maxDiscountAmount = 5000
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `카드 조건이 하나도 없으면 예외`() {
            // When & Then
            assertThatThrownBy {
                createPromotion(
                    cardBrand = null,
                    cardType = null,
                    issuerCode = null
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("카드 조건")
        }
    }

    @Nested
    @DisplayName("할인 금액 계산")
    inner class CalculateDiscountTest {

        @Test
        fun `FIXED 타입은 고정 할인 금액을 반환한다`() {
            // Given
            val promotion = createPromotion(
                discountType = DiscountType.FIXED,
                discountValue = 3000,
                maxDiscountAmount = null
            )

            // When
            val discount = promotion.calculateDiscount(10000)

            // Then
            assertThat(discount).isEqualTo(3000)
        }

        @Test
        fun `PERCENTAGE 타입은 비율 계산한 할인 금액을 반환한다`() {
            // Given
            val promotion = createPromotion(
                discountType = DiscountType.PERCENTAGE,
                discountValue = 10,  // 10%
                maxDiscountAmount = null
            )

            // When
            val discount = promotion.calculateDiscount(50000)

            // Then
            assertThat(discount).isEqualTo(5000)
        }

        @Test
        fun `PERCENTAGE 타입에서 계산 금액이 maxDiscountAmount보다 작으면 계산 금액을 반환한다`() {
            // Given
            val promotion = createPromotion(
                discountType = DiscountType.PERCENTAGE,
                discountValue = 10,  // 10%
                maxDiscountAmount = 10000
            )

            // When
            val discount = promotion.calculateDiscount(50000)

            // Then
            assertThat(discount).isEqualTo(5000)
        }

        @Test
        fun `PERCENTAGE 타입에서 계산 금액이 maxDiscountAmount보다 크면 maxDiscountAmount를 반환한다`() {
            // Given
            val promotion = createPromotion(
                discountType = DiscountType.PERCENTAGE,
                discountValue = 10,  // 10%
                maxDiscountAmount = 3000
            )

            // When
            val discount = promotion.calculateDiscount(50000)  // 10% = 5000원, but max 3000

            // Then
            assertThat(discount).isEqualTo(3000)
        }
    }

    @Nested
    @DisplayName("적용조건 매칭")
    inner class MatchesTest {

        @Test
        fun `결제수단 정보가 없으면 매칭되지 않는다`() {
            // Given
            val promotion = createPromotion()

            // When
            val matches = promotion.matches(null, 10000)

            // Then
            assertThat(matches).isFalse()
        }

        @Test
        fun `모든 적용조건을 만족하면 매칭된다`() {
            // Given
            val promotion = createPromotion(
                cardBrand = CardBrand.VISA,
                cardType = CardType.CREDIT,
                issuerCode = "04",
                minAmount = 10000
            )
            val card = createCard(
                brand = CardBrand.VISA,
                cardType = CardType.CREDIT,
                issuerCode = "04"
            )

            // When
            val matches = promotion.matches(card, 15000)

            // Then
            assertThat(matches).isTrue()
        }

        @ParameterizedTest
        @MethodSource("com.paybuddy.payment.domain.PromotionTest#프로모션적용조건불일치케이스")
        fun `하나라도 적용조건을 만족하지 않으면 매칭되지 않는다`(
            promotion: Promotion,
            card: Card,
            amount: Long
        ) {
            // When
            val matches = promotion.matches(card, amount)

            // Then
            assertThat(matches).isFalse()
        }
    }

    companion object {
        @JvmStatic
        fun 프로모션적용조건불일치케이스() = listOf(
            // 1. 나머지 일치, cardBrand만 불일치
            Arguments.of(
                createPromotion(
                    cardBrand = CardBrand.VISA,
                    cardType = CardType.CREDIT,
                    issuerCode = "04",
                    minAmount = 10000
                ),
                createCard(
                    brand = CardBrand.MASTERCARD,
                    cardType = CardType.CREDIT,
                    issuerCode = "04"
                ),
                15000L
            ),
            // 2. 나머지 일치, cardType만 불일치
            Arguments.of(
                createPromotion(
                    cardBrand = CardBrand.VISA,
                    cardType = CardType.CREDIT,
                    issuerCode = "04",
                    minAmount = 10000
                ),
                createCard(
                    brand = CardBrand.VISA,
                    cardType = CardType.DEBIT,
                    issuerCode = "04"
                ),
                15000L
            ),
            // 3. 나머지 일치, issuerCode만 불일치
            Arguments.of(
                createPromotion(
                    cardBrand = CardBrand.VISA,
                    cardType = CardType.CREDIT,
                    issuerCode = "04",
                    minAmount = 10000
                ),
                createCard(
                    brand = CardBrand.VISA,
                    cardType = CardType.CREDIT,
                    issuerCode = "06"
                ),
                15000L
            ),
            // 4. 나머지 일치, minAmount만 불일치
            Arguments.of(
                createPromotion(
                    cardBrand = CardBrand.VISA,
                    cardType = CardType.CREDIT,
                    issuerCode = "04",
                    minAmount = 10000
                ),
                createCard(
                    brand = CardBrand.VISA,
                    cardType = CardType.CREDIT,
                    issuerCode = "04"
                ),
                9999L
            )
        )

        private fun createPromotion(
            id: String = "promo_123",
            name: String = "테스트 프로모션",
            provider: PromotionProvider = PromotionProvider.PLATFORM,
            discountType: DiscountType = DiscountType.FIXED,
            discountValue: Long = 1000,
            maxDiscountAmount: Long? = null,
            cardBrand: CardBrand? = CardBrand.VISA,
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

        private fun createCard(
            maskedNumber: String = "1234-56**-****-7890",
            bin: String = "123456",
            brand: CardBrand? = CardBrand.VISA,
            issuerCode: String = "04",
            acquirerCode: String = "04",
            cardType: CardType = CardType.CREDIT,
            ownerType: OwnerType = OwnerType.PERSONAL,
            issuedCountry: String = "KR",
            productCode: String? = null
        ): Card {
            return Card(
                maskedNumber = maskedNumber,
                bin = bin,
                brand = brand,
                issuerCode = issuerCode,
                acquirerCode = acquirerCode,
                cardType = cardType,
                ownerType = ownerType,
                issuedCountry = issuedCountry,
                productCode = productCode
            )
        }
    }
}
