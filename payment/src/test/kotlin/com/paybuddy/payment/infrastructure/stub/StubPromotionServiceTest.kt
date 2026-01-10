package com.paybuddy.payment.infrastructure.stub

import com.paybuddy.payment.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StubPromotionServiceTest {

    private val sut = StubPromotionService()

    @Test
    fun `10,000원 이상 결제 시 플랫폼 프로모션이 적용된다`() {
        // Given
        val card = createCard(brand = CardBrand.MASTERCARD)
        val amount = 10_000L

        // When
        val promotions = sut.calculateEffectivePromotions("mch_123", card, amount)

        // Then
        assertThat(promotions).hasSize(1)
        assertThat(promotions[0]).isEqualTo(
            EffectivePromotion(
                name = "신규 회원 할인",
                provider = PromotionProvider.PLATFORM,
                amount = 1_000
            )
        )
    }

    @Test
    fun `10,000원 미만 결제 시 프로모션이 적용되지 않는다`() {
        // Given
        val card = createCard(brand = CardBrand.VISA)
        val amount = 9_999L

        // When
        val promotions = sut.calculateEffectivePromotions("mch_123", card, amount)

        // Then
        assertThat(promotions).isEmpty()
    }

    @Test
    fun `VISA 카드로 20,000원 이상 결제 시 두 프로모션이 모두 적용된다`() {
        // Given
        val card = createCard(brand = CardBrand.VISA)
        val amount = 20_000L

        // When
        val promotions = sut.calculateEffectivePromotions("mch_123", card, amount)

        // Then
        assertThat(promotions).hasSize(2)
        assertThat(promotions).containsExactly(
            EffectivePromotion(
                name = "VISA 카드 특별 할인",
                provider = PromotionProvider.CARD_ISSUER,
                amount = 2_000
            ),
            EffectivePromotion(
                name = "신규 회원 할인",
                provider = PromotionProvider.PLATFORM,
                amount = 1_000
            )
        )
    }

    @Test
    fun `비VISA 카드로 20,000원 이상 결제 시 플랫폼 프로모션만 적용된다`() {
        // Given
        val card = createCard(brand = CardBrand.MASTERCARD)
        val amount = 20_000L

        // When
        val promotions = sut.calculateEffectivePromotions("mch_123", card, amount)

        // Then
        assertThat(promotions).hasSize(1)
        assertThat(promotions[0].provider).isEqualTo(PromotionProvider.PLATFORM)
    }

    @Test
    fun `VISA 카드로 10,000원 이상 20,000원 미만 결제 시 플랫폼 프로모션만 적용된다`() {
        // Given
        val card = createCard(brand = CardBrand.VISA)
        val amount = 15_000L

        // When
        val promotions = sut.calculateEffectivePromotions("mch_123", card, amount)

        // Then
        assertThat(promotions).hasSize(1)
        assertThat(promotions[0].provider).isEqualTo(PromotionProvider.PLATFORM)
    }

    private fun createCard(brand: CardBrand): Card {
        return Card(
            maskedNumber = "1234********7890",
            expiryMonth = 12,
            expiryYear = 28,
            holderName = null,
            bin = "123456",
            brand = brand,
            issuerCode = "04",
            acquirerCode = "04",
            cardType = CardType.CREDIT,
            ownerType = OwnerType.PERSONAL,
            issuedCountry = "KR",
            productCode = null
        )
    }
}
