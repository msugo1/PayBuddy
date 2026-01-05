package com.paybuddy.payment.domain

import org.springframework.stereotype.Component

/**
 * 결제 시스템의 비즈니스 정책
 *
 * 현재는 전역 기본값만 제공하지만, 향후 다음과 같이 확장 예정:
 * - 상점별 차등 정책 (MerchantPaymentPolicy)
 * - 결제 수단별 정책 (카드사별 할부 제한, 간편결제 한도)
 * - 시간대/요일별 동적 정책 (프로모션 기간 최소금액 조정)
 * - DB 또는 외부 설정 저장소에서 정책 로드
 */
interface PaymentPolicy {
    val defaultExpireMinutes: Long
    val minPaymentAmount: Long
}

@Component
class DefaultPaymentPolicy : PaymentPolicy {
    override val defaultExpireMinutes = 15L
    override val minPaymentAmount = 1000L
}