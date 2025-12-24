package com.paybuddy.payment.domain

/**
 * 결제 시스템 비즈니스 정책
 */
interface PaymentPolicy {
    /**
     * 결제 세션 기본 만료 시간 (분)
     */
    val defaultExpireMinutes: Long

    /**
     * 최소 결제 금액 (원)
     */
    val minPaymentAmount: Long
}

/**
 * 기본 결제 정책 임의구현
 */
class DefaultPaymentPolicy : PaymentPolicy {
    override val defaultExpireMinutes = 15L
    override val minPaymentAmount = 1000L
}