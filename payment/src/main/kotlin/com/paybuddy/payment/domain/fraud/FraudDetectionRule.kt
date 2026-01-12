package com.paybuddy.payment.domain.fraud

import com.paybuddy.payment.domain.Card

/**
 * 부정거래 탐지 규칙
 *
 * 각 규칙을 독립적으로 구현하여 조합 가능하게 함 (Rule Pattern)
 */
interface FraudDetectionRule {
    /**
     * 규칙 검증
     *
     * @throws FraudDetectedException 규칙 위반 시
     */
    fun check(merchantId: String, card: Card, amount: Long)
}
