package com.paybuddy.payment.domain

import com.paybuddy.payment.domain.fraud.*
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class FraudDetectionServiceTest {

    private val rules = listOf(
        CountryBlockRule(),
        VelocityRule()
    )
    private val sut: FraudDetectionService = FraudDetectionService(rules)

    @Test
    fun `국내 발급 카드는 FDS를 통과한다`() {
        // Given
        val card = createCard(issuedCountry = "KR")

        // When & Then
        assertThatCode {
            sut.check("mch_123", card, 10000)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `해외 발급 카드는 차단된다`() {
        // Given
        val card = createCard(issuedCountry = "US")

        // When & Then
        assertThatThrownBy {
            sut.check("mch_123", card, 10000)
        }.isInstanceOf(FraudDetectedException::class.java)
            .hasFieldOrPropertyWithValue("reason", "COUNTRY_BLOCKED")
    }

    private fun createCard(issuedCountry: String = "KR"): Card {
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
