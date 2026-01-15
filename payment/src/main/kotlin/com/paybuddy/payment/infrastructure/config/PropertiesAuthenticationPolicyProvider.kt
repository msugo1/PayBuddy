package com.paybuddy.payment.infrastructure.config

import com.paybuddy.payment.domain.authentication.AuthenticationPolicyProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "payment.authentication")
class PropertiesAuthenticationPolicyProvider : AuthenticationPolicyProvider {
    override lateinit var exemptionCountries: Set<String>
    override var highAmountThreshold: Long = 300_000L
}
