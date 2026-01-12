package com.paybuddy.payment.infrastructure.config

import com.paybuddy.payment.domain.fraud.AllowedCountryProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "fraud.country-block")
class PropertiesAllowedCountryProvider : AllowedCountryProvider {
    lateinit var countries: Set<String>

    override fun getAllowedCountries(): Set<String> = countries
}
