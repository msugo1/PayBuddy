package com.paybuddy.payment.infrastructure.stub

import com.paybuddy.payment.domain.AuthenticationRule
import com.paybuddy.payment.domain.Card
import org.springframework.stereotype.Component

/**
 * 인증 규칙 Stub 구현 (1차)
 *
 * 3DS 인증 필요 조건:
 * 1. 해외 발급 카드 (KR 외)
 * 2. 고액 결제 (30만원 이상)
 *
 * 면제 조건:
 * 1. 국내 발급 카드 + 30만원 미만
 */
@Component
class StubAuthenticationRule : AuthenticationRule {

    companion object {
        private const val HIGH_AMOUNT_THRESHOLD = 300_000L
        private val ALLOWED_COUNTRIES_WITHOUT_AUTH = setOf("KR")
    }

    override fun requiresAuthentication(
        card: Card,
        amount: Long,
        merchantId: String
    ): Boolean {
        // 해외 발급 카드는 항상 인증 필요
        if (card.issuedCountry !in ALLOWED_COUNTRIES_WITHOUT_AUTH) {
            return true
        }

        // 국내 카드 + 고액 결제는 인증 필요
        if (amount >= HIGH_AMOUNT_THRESHOLD) {
            return true
        }

        // 국내 카드 + 30만원 미만은 인증 불필요
        return false
    }
}
