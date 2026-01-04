package com.paybuddy.payment.infrastructure.redis

import com.paybuddy.payment.domain.ExclusivePaymentGate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class RedisExclusivePaymentGate(
    private val redisTemplate: StringRedisTemplate,
    private val properties: PaymentGateProperties
) : ExclusivePaymentGate {

    /**
     * 서버 인스턴스별 고유 ID
     * threadId와 조합하여 스레드별 락 소유권 식별 (다른 인스턴스/스레드의 락 삭제 방지)
     */
    private val instanceId = UUID.randomUUID().toString()

    override fun tryEnter(merchantId: String, orderId: String): Boolean {
        val key = generatePaymentGateKey(merchantId, orderId)
        val owner = "$instanceId:${Thread.currentThread().threadId()}"
        val ttl = Duration.ofSeconds(properties.lockTtlSeconds)

        return redisTemplate.opsForValue()
            .setIfAbsent(key, owner, ttl) ?: false
    }

    override fun exit(merchantId: String, orderId: String) {
        val key = generatePaymentGateKey(merchantId, orderId)
        val expectedOwner = "$instanceId:${Thread.currentThread().threadId()}"
        val currentOwner = redisTemplate.opsForValue().get(key)

        // 자신이 획득한 락만 해제 (TTL 만료 후 다른 소유자 보호)
        if (currentOwner == expectedOwner) {
            redisTemplate.delete(key)
        }
    }

    private fun generatePaymentGateKey(merchantId: String, orderId: String): String {
        return "payment:gate:$merchantId:$orderId"
    }
}
