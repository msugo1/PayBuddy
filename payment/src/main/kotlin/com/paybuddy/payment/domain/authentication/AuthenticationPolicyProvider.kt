package com.paybuddy.payment.domain.authentication

/**
 * 인증 정책 제공
 *
 * 구현체:
 * - 1차: Properties 기반
 * - 2차: DB 기반 (재배포 없이 동적 관리)
 */
interface AuthenticationPolicyProvider {
    fun getHighAmountThreshold(): Long
    fun getExemptionCountries(): Set<String>
}
