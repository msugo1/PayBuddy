package com.paybuddy.payment.domain

/**
 * 카드 결제 상태
 */
enum class CardPaymentStatus {
    /**
     * PG 승인 요청 중
     */
    IN_PROGRESS,

    /**
     * 가승인 완료 (한도 홀드, 돈 미이동)
     */
    AUTHORIZED,

    /**
     * 매입 완료 (실제 이체)
     */
    CAPTURED,

    /**
     * 실패/타임아웃
     */
    FAILED,

    /**
     * 가승인 취소
     */
    VOIDED,

    /**
     * 환불
     */
    REFUNDED;

    /**
     * 주어진 상태로 전이 가능한지 검증
     *
     * @param newStatus 전이하려는 새로운 상태
     * @return 전이 가능 여부
     */
    fun canTransitionTo(newStatus: CardPaymentStatus): Boolean {
        return when (this) {
            IN_PROGRESS -> newStatus in setOf(AUTHORIZED, CAPTURED, FAILED)
            AUTHORIZED -> newStatus in setOf(CAPTURED, VOIDED)
            CAPTURED -> newStatus == REFUNDED
            else -> false
        }
    }
}