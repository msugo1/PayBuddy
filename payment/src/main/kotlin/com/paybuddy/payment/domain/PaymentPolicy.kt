package com.paybuddy.payment.domain

/**
 * 결제 시스템 비즈니스 정책 상수
 */
object PaymentPolicy {
    /**
     * 결제 세션 기본 만료 시간 (분)
     */
    const val DEFAULT_EXPIRE_MINUTES = 15L

    /**
     * 결제 세션 최소 만료 시간 (분)
     */
    const val MIN_EXPIRE_MINUTES = 5L

    /**
     * 결제 세션 최대 만료 시간 (분)
     */
    const val MAX_EXPIRE_MINUTES = 60L

    /**
     * 최소 결제 금액 (원)
     */
    const val MIN_PAYMENT_AMOUNT = 1000L
}