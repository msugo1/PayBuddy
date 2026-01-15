package com.paybuddy.payment.domain.authentication

import com.paybuddy.payment.application.dto.AuthenticationRedirect
import com.paybuddy.payment.domain.Card

sealed class AuthenticationResult {
    data class Required(val redirect: AuthenticationRedirect) : AuthenticationResult()
    data object NotRequired : AuthenticationResult()
}

interface AuthenticationService {
    fun prepareAuthentication(
        card: Card,
        amount: Long,
        merchantId: String
    ): AuthenticationResult
}
