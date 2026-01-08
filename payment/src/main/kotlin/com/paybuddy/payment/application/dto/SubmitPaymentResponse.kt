package com.paybuddy.payment.application.dto

/**
 * Submit 응답
 */
data class SubmitPaymentResponse(
    val paymentKey: String,
    val status: SubmitStatus,
    val authentication: AuthenticationInfo? = null,
    val redirectUrl: String? = null
)

enum class SubmitStatus {
    AUTHENTICATION_REQUIRED,
    PENDING_CONFIRM
}

/**
 * 인증 정보 (3DS)
 */
data class AuthenticationInfo(
    val type: AuthenticationType,
    val method: AuthenticationMethod,
    val url: String,
    val data: Map<String, String>
)

enum class AuthenticationType {
    THREE_D_SECURE,
    ACS_REDIRECT
}

enum class AuthenticationMethod {
    POST,
    GET,
    IFRAME
}
