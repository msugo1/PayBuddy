package com.paybuddy.payment.infrastructure.persistence

import com.paybuddy.payment.domain.PaymentMethodType
import com.paybuddy.payment.domain.Promotion
import com.paybuddy.payment.domain.PromotionRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class PromotionRepositoryAdapter(
    private val jpaPromotionRepository: JpaPromotionRepository
) : PromotionRepository {

    override fun findActivePromotions(paymentMethodType: PaymentMethodType): List<Promotion> {
        val now = Instant.now()
        return jpaPromotionRepository.findByValidFromLessThanEqualAndValidUntilGreaterThanEqual(now, now)
    }
}
