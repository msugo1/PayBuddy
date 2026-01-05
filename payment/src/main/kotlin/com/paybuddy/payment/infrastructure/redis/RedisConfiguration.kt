package com.paybuddy.payment.infrastructure.redis

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(PaymentGateProperties::class)
class RedisConfiguration
