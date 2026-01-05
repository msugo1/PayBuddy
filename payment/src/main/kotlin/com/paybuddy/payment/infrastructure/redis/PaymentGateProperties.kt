package com.paybuddy.payment.infrastructure.redis

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "payment.gate")
data class PaymentGateProperties(
    /** 락 자동 해제 시간 (초) - finally 블록 미도달 시 안전장치 */
    val lockTtlSeconds: Long = 5
)
