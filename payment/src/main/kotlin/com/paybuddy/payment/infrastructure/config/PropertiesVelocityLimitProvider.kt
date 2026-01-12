package com.paybuddy.payment.infrastructure.config

import com.paybuddy.payment.domain.fraud.VelocityLimitProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "fraud.velocity")
class PropertiesVelocityLimitProvider : VelocityLimitProvider {
    var limit: Int = 5

    override fun getMaxTransactionsPerMinute(): Int = limit
}
