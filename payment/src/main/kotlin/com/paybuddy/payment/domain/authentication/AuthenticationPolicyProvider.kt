package com.paybuddy.payment.domain.authentication

/**
 * 인증 정책 제공
 *
 * 구현체:
 * - 1차: Properties 기반
 * - 2차: DB 기반 (재배포 없이 동적 관리)
 */
interface AuthenticationPolicyProvider {
    /**
     * 고액 결제 기준 금액 조회
     */
    fun getHighAmountThreshold(): Long

    /**
     * 인증 면제 국가 목록 조회
     */
    fun getExemptionCountries(): Set<String>
}
