package com.paybuddy.payment.application.dto

/**
 * Submit 응답
 */
data class SubmitPaymentResponse(
    val paymentKey: String,
    val status: SubmitStatus,
    val authentication: AuthenticationRedirect? = null,
    val redirectUrl: String? = null
)

enum class SubmitStatus {
    AUTHENTICATION_REQUIRED,
    PENDING_CONFIRM
}

/**
 * 인증 리다이렉트 정보
 *
 * 3DS, ISP, 간편결제 등 리다이렉트 기반 인증에 필요한 정보
 */
data class AuthenticationRedirect(
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
