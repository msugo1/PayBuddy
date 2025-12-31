package com.paybuddy.payment.domain

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.OffsetDateTime

/**
 * 결제 준비 시 생성되며 expireAt까지 유효하다.
 * merchantId + orderId로 중복 요청을 방지한다.
 *
 * PaymentSessionFactory를 통해 생성한다.
 */
@Entity
@Table(name = "payment_session")
class PaymentSession(
    @Id
    @Column(length = 26)
    val id: String,  // ULID (= 외부 paymentKey)

    @Column(nullable = false)
    val merchantId: String,

    @Column(nullable = false)
    val orderId: String,

    @Type(JsonBinaryType::class)
    @Column(nullable = false, columnDefinition = "jsonb")
    val orderLine: OrderLine,

    @Embedded
    val amount: PaymentAmount,

    @Column(nullable = false)
    val expiresAt: OffsetDateTime,

    @Embedded
    val redirectUrl: RedirectUrl,

    @Column(nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
) {
    @Column(nullable = false)
    var expired: Boolean = false
        private set

    @PreUpdate
    fun preUpdate() {
        this.updatedAt = OffsetDateTime.now()
    }

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
