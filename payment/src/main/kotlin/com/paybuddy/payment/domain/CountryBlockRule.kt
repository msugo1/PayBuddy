package com.paybuddy.payment.domain

import org.springframework.stereotype.Component

/**
 * 국가 차단 규칙
 *
 * 허용된 국가에서 발급된 카드만 사용 가능
 */
@Component
class CountryBlockRule : FraudDetectionRule {

    companion object {
        private val ALLOWED_COUNTRIES = setOf("KR")
    }

    override fun check(merchantId: String, card: Card, amount: Long) {
        if (card.issuedCountry !in ALLOWED_COUNTRIES) {
            throw FraudDetectedException(
                reason = "COUNTRY_BLOCKED",
                message = "해외 발급 카드는 사용할 수 없습니다: ${card.issuedCountry}"
            )
        }
    }
}
