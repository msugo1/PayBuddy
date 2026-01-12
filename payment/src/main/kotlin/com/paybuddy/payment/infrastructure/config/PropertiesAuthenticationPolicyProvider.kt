package com.paybuddy.payment.infrastructure.config

import com.paybuddy.payment.domain.authentication.AuthenticationPolicyProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "payment.authentication")
class PropertiesAuthenticationPolicyProvider : AuthenticationPolicyProvider {

    lateinit var exemptionCountries: Set<String>
    var highAmountThreshold: Long = 300_000L

    override fun getHighAmountThreshold(): Long = highAmountThreshold

    override fun getExemptionCountries(): Set<String> = exemptionCountries
}
