package com.paybuddy.payment.domain

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import java.time.OffsetDateTime

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

    @Embedded
    val installment: Installment?,

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
