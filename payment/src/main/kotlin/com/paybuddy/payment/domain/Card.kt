package com.paybuddy.payment.domain

data class Card(
    val maskedNumber: String,
    val bin: String,
    val brand: CardBrand?,
    val issuerCode: String,
    val acquirerCode: String,
    val cardType: CardType,
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
