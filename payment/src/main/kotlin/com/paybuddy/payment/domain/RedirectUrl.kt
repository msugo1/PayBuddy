package com.paybuddy.payment.domain

/**
 * 결제 프로세스 완료 후 리다이렉트 URL
 */
data class RedirectUrl(
    val success: String,
    val fail: String
)