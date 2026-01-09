package com.paybuddy.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

class PromotionTest {

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
        fun `FIXED 타입에서 maxDiscountAmount보다 작으면 discountValue를 반환한다`() {
            // Given
            val promotion = createPromotion(
                discountType = DiscountType.FIXED,
                discountValue = 3000,
                maxDiscountAmount = 5000
            )

            // When
            val discount = promotion.calculateDiscount(10000)

            // Then
            assertThat(discount).isEqualTo(3000)
        }

        @Test
        fun `FIXED 타입에서 maxDiscountAmount보다 크면 maxDiscountAmount를 반환한다`() {
            // Given
            val promotion = createPromotion(
                discountType = DiscountType.FIXED,
                discountValue = 7000,
                maxDiscountAmount = 5000
            )

            // When
            val discount = promotion.calculateDiscount(10000)

            // Then
            assertThat(discount).isEqualTo(5000)
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
    @DisplayName("조건 매칭")
    inner class MatchesTest {

        @Test
        fun `card가 null이면 false를 반환한다`() {
            // Given
            val promotion = createPromotion()

            // When
            val matches = promotion.matches(null, 10000)

            // Then
            assertThat(matches).isFalse()
        }

        @Test
        fun `모든 조건이 null이면 true를 반환한다`() {
            // Given
            val promotion = createPromotion(
                cardBrand = null,
                cardType = null,
                issuerCode = null,
                minAmount = null
            )
            val card = createCard()

            // When
            val matches = promotion.matches(card, 10000)

            // Then
            assertThat(matches).isTrue()
        }

        @Test
        fun `cardBrand가 일치하면 true를 반환한다`() {
            // Given
            val promotion = createPromotion(cardBrand = CardBrand.VISA)
            val card = createCard(brand = CardBrand.VISA)

            // When
            val matches = promotion.matches(card, 10000)

            // Then
            assertThat(matches).isTrue()
        }

        @Test
        fun `cardBrand가 불일치하면 false를 반환한다`() {
            // Given
            val promotion = createPromotion(cardBrand = CardBrand.VISA)
            val card = createCard(brand = CardBrand.MASTERCARD)

            // When
            val matches = promotion.matches(card, 10000)

            // Then
            assertThat(matches).isFalse()
        }

        @Test
        fun `cardType이 일치하면 true를 반환한다`() {
            // Given
            val promotion = createPromotion(cardType = CardType.CREDIT)
            val card = createCard(cardType = CardType.CREDIT)

            // When
            val matches = promotion.matches(card, 10000)

            // Then
            assertThat(matches).isTrue()
        }

        @Test
        fun `cardType이 불일치하면 false를 반환한다`() {
            // Given
            val promotion = createPromotion(cardType = CardType.CREDIT)
            val card = createCard(cardType = CardType.DEBIT)

            // When
            val matches = promotion.matches(card, 10000)

            // Then
            assertThat(matches).isFalse()
        }

        @Test
        fun `issuerCode가 일치하면 true를 반환한다`() {
            // Given
            val promotion = createPromotion(issuerCode = "04")
            val card = createCard(issuerCode = "04")

            // When
            val matches = promotion.matches(card, 10000)

            // Then
            assertThat(matches).isTrue()
        }

        @Test
        fun `issuerCode가 불일치하면 false를 반환한다`() {
            // Given
            val promotion = createPromotion(issuerCode = "04")
            val card = createCard(issuerCode = "06")

            // When
            val matches = promotion.matches(card, 10000)

            // Then
            assertThat(matches).isFalse()
        }

        @Test
        fun `금액이 minAmount 이상이면 true를 반환한다`() {
            // Given
            val promotion = createPromotion(minAmount = 10000)
            val card = createCard()

            // When
            val matches = promotion.matches(card, 15000)

            // Then
            assertThat(matches).isTrue()
        }

        @Test
        fun `금액이 minAmount 미만이면 false를 반환한다`() {
            // Given
            val promotion = createPromotion(minAmount = 10000)
            val card = createCard()

            // When
            val matches = promotion.matches(card, 9000)

            // Then
            assertThat(matches).isFalse()
        }

        @Test
        fun `모든 조건이 일치하면 true를 반환한다`() {
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

        @Test
        fun `하나라도 조건이 불일치하면 false를 반환한다`() {
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
                issuerCode = "06"  // 발급사 불일치
            )

            // When
            val matches = promotion.matches(card, 15000)

            // Then
            assertThat(matches).isFalse()
        }
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
