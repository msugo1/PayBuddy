package com.paybuddy.payment.infrastructure.stub

import com.paybuddy.payment.domain.Card
import com.paybuddy.payment.domain.authentication.AuthenticationPolicyProvider
import com.paybuddy.payment.domain.authentication.AuthenticationRule
import org.springframework.stereotype.Component

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

        if (card.issuedCountry !in exemptionCountries) {
            return true
        }

        if (amount >= highAmountThreshold) {
            return true
        }

        return false
    }
}
