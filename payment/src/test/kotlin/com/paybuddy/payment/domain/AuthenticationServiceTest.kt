package com.paybuddy.payment.domain

import com.paybuddy.payment.domain.authentication.AuthenticationPolicyProvider
import com.paybuddy.payment.domain.authentication.AuthenticationResult
import com.paybuddy.payment.domain.authentication.AuthenticationService
import com.paybuddy.payment.infrastructure.stub.StubAuthenticationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AuthenticationServiceTest {

    private val policyProvider = object : AuthenticationPolicyProvider {
        override val highAmountThreshold: Long = 300_000L
        override val exemptionCountries: Set<String> = setOf("KR")
    }

    private val sut: AuthenticationService = StubAuthenticationService(policyProvider)

    @ParameterizedTest
    @CsvSource(
        // 인증 필요
        "US, 10000",      // 면제되지 않은 국가 - 소액
        "US, 300000",     // 면제되지 않은 국가 - 고액
        "KR, 300000"      // 면제 국가 - 기준 금액 이상 (경계값)
    )
    fun `인증이 필요하다`(issuedCountry: String, amount: Long) {
        // Given
        val card = createCard(issuedCountry)

        // When
        val result = sut.prepareAuthentication(card, amount, "mch_123")

        // Then
        assertThat(result).isInstanceOf(AuthenticationResult.Required::class.java)
    }

    @ParameterizedTest
    @CsvSource(
        // 인증 불필요
        "KR, 299999",    // 면제 국가 - 기준 금액 미만 (경계값)
        "KR, 50000"      // 면제 국가 - 소액
    )
    fun `인증이 불필요하다`(issuedCountry: String, amount: Long) {
        // Given
        val card = createCard(issuedCountry)

        // When
        val result = sut.prepareAuthentication(card, amount, "mch_123")

        // Then
        assertThat(result).isEqualTo(AuthenticationResult.NotRequired)
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
