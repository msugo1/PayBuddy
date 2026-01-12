package com.paybuddy.payment.domain.authentication

import com.paybuddy.payment.domain.Card

/**
 * 결제 인증 필요 여부 판단 규칙
 *
 * 3DS(3D Secure) 인증 필요 여부를 결정
 */
interface AuthenticationRule {
    /**
     * 인증 필요 여부 판단
     *
     * @return true: 인증 필요, false: 인증 불필요
     */
    fun requiresAuthentication(
        card: Card,
        amount: Long,
        merchantId: String
    ): Boolean
}
