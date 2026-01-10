package com.paybuddy.payment.domain

data class Bin(
    val number: String,
    val brand: CardBrand?,
    val issuerCode: String,
    val acquirerCode: String,
    val cardType: CardType,
    val ownerType: OwnerType,
    val issuedCountry: String,
    val productCode: String?
)

interface BinLookupService {
    fun lookup(cardNumber: String): Bin
}
