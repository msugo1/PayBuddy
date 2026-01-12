package com.paybuddy.payment.infrastructure.stub

import com.paybuddy.payment.domain.PaymentMethodType
import com.paybuddy.payment.domain.merchant.MerchantLimitService
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class StubMerchantLimitService : MerchantLimitService {
    private val defaultLimit = 100_000_000L
    private val consumed = ConcurrentHashMap<String, Long>()

    override fun check(merchantId: String, paymentMethod: PaymentMethodType, amount: Long): Boolean {
        val currentConsumed = consumed.getOrDefault(merchantId, 0L)
        return (currentConsumed + amount) <= defaultLimit
    }

    override fun consume(merchantId: String, paymentId: String, amount: Long) {
        consumed.merge(merchantId, amount, Long::plus)
    }

    override fun restore(merchantId: String, paymentId: String, amount: Long) {
        consumed.merge(merchantId, -amount, Long::plus)
    }
}
