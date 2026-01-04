package com.paybuddy.payment.domain

/** 같은 주문에 대한 동시 결제 세션 생성 방지 (fail-fast) */
interface ExclusivePaymentGate {
    fun tryEnter(merchantId: String, orderId: String): Boolean
    fun exit(merchantId: String, orderId: String)
}
