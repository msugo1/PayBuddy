package com.paybuddy.payment.domain

enum class PaymentStatus {
    INITIALIZED,
    AUTHENTICATION_REQUIRED,
    PENDING_CONFIRM,
    PAYMENT_PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED;

    fun transitionTo(next: PaymentStatus): PaymentStatus {
        require((this to next) in allowedTransitions) { "상태 전이 불가: $this → $next" }
        return next
    }

    companion object {
        private val allowedTransitions = setOf(
            INITIALIZED to AUTHENTICATION_REQUIRED,
            INITIALIZED to PENDING_CONFIRM,
            INITIALIZED to FAILED,
            AUTHENTICATION_REQUIRED to PENDING_CONFIRM,
            AUTHENTICATION_REQUIRED to FAILED,
            PENDING_CONFIRM to PAYMENT_PROCESSING,
            PENDING_CONFIRM to CANCELLED,
            PAYMENT_PROCESSING to COMPLETED,
            PAYMENT_PROCESSING to FAILED,
        )
    }
}
