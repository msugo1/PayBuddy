package com.paybuddy.payment.domain

import com.paybuddy.payment.infrastructure.stub.StubAuthenticationRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AuthenticationRuleTest {

    private val sut: AuthenticationRule = StubAuthenticationRule()

    @Test
    fun `해외 발급 카드는 금액과 무관하게 인증이 필요하다`() {
        // Given
        val card = createCard(issuedCountry = "US")

        // When
        val result = sut.requiresAuthentication(card, 10_000, "mch_123")

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `국내 카드로 30만원 이상 결제 시 인증이 필요하다`() {
        // Given
        val card = createCard(issuedCountry = "KR")

        // When
        val result = sut.requiresAuthentication(card, 300_000, "mch_123")

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `국내 카드로 30만원 미만 결제 시 인증이 불필요하다`() {
        // Given
        val card = createCard(issuedCountry = "KR")

        // When
        val result = sut.requiresAuthentication(card, 299_999, "mch_123")

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `국내 카드로 정확히 30만원 결제 시 인증이 필요하다`() {
        // Given
        val card = createCard(issuedCountry = "KR")

        // When
        val result = sut.requiresAuthentication(card, 300_000, "mch_123")

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `국내 카드로 소액 결제 시 인증이 불필요하다`() {
        // Given
        val card = createCard(issuedCountry = "KR")

        // When
        val result = sut.requiresAuthentication(card, 50_000, "mch_123")

        // Then
        assertThat(result).isFalse()
    }

    private fun createCard(issuedCountry: String): Card {
        return Card(
            maskedNumber = "1234********7890",
            expiryMonth = 12,
            expiryYear = 28,
            holderName = null,
            bin = "123456",
            brand = CardBrand.VISA,
            issuerCode = "04",
            acquirerCode = "04",
            cardType = CardType.CREDIT,
            ownerType = OwnerType.PERSONAL,
            issuedCountry = issuedCountry,
            productCode = null
        )
    }
}
