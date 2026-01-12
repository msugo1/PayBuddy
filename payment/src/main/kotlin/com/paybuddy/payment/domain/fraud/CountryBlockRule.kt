package com.paybuddy.payment.domain.fraud

import com.paybuddy.payment.domain.Card
import org.springframework.stereotype.Component

/**
 * 국가 차단 규칙
 *
 * 허용된 국가에서 발급된 카드만 사용 가능
 */
@Component
class CountryBlockRule(
    private val allowedCountryProvider: AllowedCountryProvider
) : FraudDetectionRule {

    override fun check(merchantId: String, card: Card, amount: Long) {
        val allowedCountries = allowedCountryProvider.getAllowedCountries()

        if (card.issuedCountry !in allowedCountries) {
            throw FraudDetectedException(
                reason = "COUNTRY_NOT_ALLOWED",
                message = "허용되지 않은 국가에서 발급된 카드입니다: ${card.issuedCountry}"
            )
        }
    }
}
