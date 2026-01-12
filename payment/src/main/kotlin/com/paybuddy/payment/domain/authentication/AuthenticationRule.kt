package com.paybuddy.payment.domain.authentication

import com.paybuddy.payment.domain.Card

interface AuthenticationRule {
    fun requiresAuthentication(
        card: Card,
        amount: Long,
        merchantId: String
    ): Boolean
}
