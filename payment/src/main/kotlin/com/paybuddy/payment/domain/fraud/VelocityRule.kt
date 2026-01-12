package com.paybuddy.payment.domain.fraud

import com.paybuddy.payment.domain.Card
import org.springframework.stereotype.Component

/**
 * Velocity 규칙 (동일 카드 단시간 반복 결제 차단)
 *
 * TODO: 실제 구현 필요 (현재는 항상 통과)
 */
@Component
class VelocityRule(
    private val velocityLimitProvider: VelocityLimitProvider
) : FraudDetectionRule {

    override fun check(merchantId: String, card: Card, amount: Long) {
        val limit = velocityLimitProvider.getMaxTransactionsPerMinute()

        // TODO: Velocity check 구현
        // 예: 동일 카드로 N분 내 limit건 이상 결제 시도 차단
    }
}
