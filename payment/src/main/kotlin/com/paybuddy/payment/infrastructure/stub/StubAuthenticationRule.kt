package com.paybuddy.payment.infrastructure.stub

import com.paybuddy.payment.domain.Card
import com.paybuddy.payment.domain.authentication.AuthenticationPolicyProvider
import com.paybuddy.payment.domain.authentication.AuthenticationRule
import org.springframework.stereotype.Component

/**
 * 인증 규칙 Stub 구현 (1차)
 *
 * 3DS 인증 필요 조건:
 * 1. 해외 발급 카드 (면제 국가 외)
 * 2. 고액 결제 (기준 금액 이상)
 *
 * 면제 조건:
 * 1. 면제 국가 발급 카드 + 기준 금액 미만
 */
@Component
class StubAuthenticationRule(
    private val policyProvider: AuthenticationPolicyProvider
) : AuthenticationRule {

    override fun requiresAuthentication(
        card: Card,
        amount: Long,
        merchantId: String
    ): Boolean {
        val exemptionCountries = policyProvider.getExemptionCountries()
        val highAmountThreshold = policyProvider.getHighAmountThreshold()

        // 해외 발급 카드는 항상 인증 필요
        if (card.issuedCountry !in exemptionCountries) {
            return true
        }

        // 국내 카드 + 고액 결제는 인증 필요
        if (amount >= highAmountThreshold) {
            return true
        }

        // 국내 카드 + 기준 금액 미만은 인증 불필요
        return false
    }
}
