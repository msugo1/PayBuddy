package com.paybuddy.payment.domain.fraud

/**
 * Velocity 제한 설정 제공
 *
 * 구현체:
 * - 1차: Properties 기반
 * - 2차: DB 기반 (재배포 없이 동적 관리)
 */
interface VelocityLimitProvider {
    fun getMaxTransactionsPerMinute(): Int
}
