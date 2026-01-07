package com.paybuddy.payment.domain

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import java.time.OffsetDateTime

// TODO: 가상계좌/간편결제 추가 시 sealed interface PaymentDetails로 리팩토링
//       - sealed interface PaymentDetails
//       - CardPaymentDetails, VirtualAccountDetails, EasyPayDetails 등
//       현재는 1차 카드만 구현하여 단일 클래스로 단순화
@Embeddable
data class CardPaymentDetails(
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "maskedNumber", column = Column(name = "card_masked_number")),
        AttributeOverride(name = "expiryMonth", column = Column(name = "card_expiry_month")),
        AttributeOverride(name = "expiryYear", column = Column(name = "card_expiry_year")),
        AttributeOverride(name = "holderName", column = Column(name = "card_holder_name")),
        AttributeOverride(name = "bin", column = Column(name = "card_bin")),
        AttributeOverride(name = "brand", column = Column(name = "card_brand")),
        AttributeOverride(name = "issuerCode", column = Column(name = "card_issuer_code")),
        AttributeOverride(name = "acquirerCode", column = Column(name = "card_acquirer_code")),
        AttributeOverride(name = "cardType", column = Column(name = "card_type")),
        AttributeOverride(name = "ownerType", column = Column(name = "card_owner_type")),
        AttributeOverride(name = "issuedCountry", column = Column(name = "card_issued_country")),
        AttributeOverride(name = "productCode", column = Column(name = "card_product_code")),
    )
    val card: Card,

    @Column(name = "installment_months")
    val installmentMonths: Int,

    @Embedded
    val result: PaymentResult? = null,
)

@Embeddable
data class PaymentResult(
    @Column(name = "approval_number")
    val approvalNumber: String? = null,

    @Column(name = "approved_at")
    val approvedAt: OffsetDateTime? = null,

    @Column(name = "error_code")
    val errorCode: String? = null,

    @Column(name = "failure_reason")
    val failureReason: String? = null,
)
