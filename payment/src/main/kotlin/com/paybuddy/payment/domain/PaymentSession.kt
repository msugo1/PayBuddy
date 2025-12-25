package com.paybuddy.payment.domain

import java.time.OffsetDateTime

/**
 * 결제 준비 시 생성되며 expireAt까지 유효하다.
 * merchantId + orderId로 중복 요청을 방지한다.
 *
 * PaymentSessionFactory를 통해 생성한다.
 */
class PaymentSession(
    val id: Long = 0,
    val paymentKey: String,
    val merchantId: String,
    val orderId: String,
    val orderLine: OrderLine,
    val amount: PaymentAmount,
    val expiresAt: OffsetDateTime,
    val redirectUrl: RedirectUrl,
    val createdAt: OffsetDateTime = OffsetDateTime.now()
) {
    var expired: Boolean = false
        private set

    fun hasReachedExpiration(currentTime: OffsetDateTime): Boolean = currentTime.isAfter(expiresAt)

    /**
     * 주문 동일성 검증
     *
     * 같은 주문에 대한 재시도 시 금액 변조 여부 확인
     */
    fun isIdenticalPayment(
        merchantId: String,
        orderId: String,
        amount: PaymentAmount
    ): Boolean {
        return this.merchantId == merchantId &&
               this.orderId == orderId &&
               this.amount == amount
    }

    fun expire() {
        this.expired = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaymentSession) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
