package com.paybuddy.payment.domain

/**
 * FDS 검증 실패 예외
 *
 * @param reason 차단 사유 (COUNTRY_BLOCKED, VELOCITY_EXCEEDED 등)
 */
class FraudDetectedException(
    val reason: String,
    message: String
) : RuntimeException(message)
