package com.paybuddy.payment.domain.fraud

import com.paybuddy.payment.domain.Card

import org.springframework.stereotype.Service

@Service
class FraudDetectionService(
    private val rules: List<FraudDetectionRule>
) {

    fun check(merchantId: String, card: Card, amount: Long) {
        rules.forEach { rule ->
            rule.check(merchantId, card, amount)
        }
    }
}