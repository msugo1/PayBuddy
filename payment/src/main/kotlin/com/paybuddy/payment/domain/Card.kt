package com.paybuddy.payment.domain

import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class Card(
    val maskedNumber: String,
    val bin: String,
    @Enumerated(EnumType.STRING)
    val brand: CardBrand?,
    val issuerCode: String,
    val acquirerCode: String,
    @Enumerated(EnumType.STRING)
    val cardType: CardType,
    @Enumerated(EnumType.STRING)
    val ownerType: OwnerType,
    val issuedCountry: String,
    val productCode: String?
)

enum class CardBrand {
    VISA,
    MASTERCARD,
    AMEX,
    JCB,
    UNIONPAY,
    BC,
    LOCAL
}

enum class CardType {
    CREDIT,
    DEBIT,
    PREPAID
}

enum class OwnerType {
    PERSONAL,
    CORPORATE
}
