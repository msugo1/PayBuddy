package com.paybuddy.payment.infrastructure

import com.paybuddy.payment.domain.PaymentMethodType
import com.paybuddy.payment.domain.Promotion
import com.paybuddy.payment.domain.PromotionRepository
import org.springframework.stereotype.Repository

@Repository
class StubPromotionRepository : PromotionRepository {
    override fun findActivePromotions(paymentMethodType: PaymentMethodType): List<Promotion> {
        return emptyList()
    }
}
