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

    companion object {
        private val allowedTransitions = setOf(
            IN_PROGRESS to AUTHORIZED,
            IN_PROGRESS to CAPTURED,
            IN_PROGRESS to FAILED,
            AUTHORIZED to CAPTURED,
            AUTHORIZED to VOIDED,
            CAPTURED to REFUNDED
        )

        /**
         * 주어진 상태 전이가 허용되는지 검증
         *
         * @param from 현재 상태
         * @param to 전이하려는 상태
         * @return 전이 가능 여부
         */
        fun canTransition(from: CardPaymentStatus, to: CardPaymentStatus): Boolean {
            return (from to to) in allowedTransitions
        }
    }
}