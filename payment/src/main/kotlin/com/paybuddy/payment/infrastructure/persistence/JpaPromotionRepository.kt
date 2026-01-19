package com.paybuddy.payment.infrastructure.persistence

import com.paybuddy.payment.domain.Promotion
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface JpaPromotionRepository : JpaRepository<Promotion, String> {

    fun findByValidFromLessThanEqualAndValidUntilGreaterThanEqual(
        validFrom: Instant,
        validUntil: Instant
    ): List<Promotion>
}
