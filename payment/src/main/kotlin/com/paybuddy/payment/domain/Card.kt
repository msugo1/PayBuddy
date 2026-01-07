package com.paybuddy.payment.domain

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class Card(
    val maskedNumber: String,
    val expiryMonth: Int? = null,
    val expiryYear: Int? = null,
    val holderName: String? = null,
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
) {
    companion object {
        fun create(
            cardNumber: String,
            expiryMonth: Int,
            expiryYear: Int,
            holderName: String?,
            binData: Bin
        ): Card {
            validateCardNumber(cardNumber)
            validateExpiry(expiryMonth, expiryYear)

            return Card(
                maskedNumber = maskCardNumber(cardNumber),
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                holderName = holderName,
                bin = binData.number,
                brand = binData.brand,
                issuerCode = binData.issuerCode,
                acquirerCode = binData.acquirerCode,
                cardType = binData.cardType,
                ownerType = binData.ownerType,
                issuedCountry = binData.issuedCountry,
                productCode = binData.productCode
            )
        }

        private fun validateCardNumber(cardNumber: String) {
            val digitsOnly = cardNumber.replace(Regex("[^0-9]"), "")

            require(digitsOnly.length in 13..19) { "유효하지 않은 카드 번호 길이입니다" }
            require(isValidLuhn(digitsOnly)) { "유효하지 않은 카드 번호입니다 (체크섬 오류)" }
        }

        private fun validateExpiry(month: Int, year: Int) {
            require(month in 1..12) { "유효하지 않은 만료 월입니다: $month" }
            require(year >= 0) { "유효하지 않은 만료 연도입니다: $year" }

            val now = java.time.YearMonth.now()
            val fullYear = if (year < 100) 2000 + year else year
            val expiryYearMonth = java.time.YearMonth.of(fullYear, month)

            require(!expiryYearMonth.isBefore(now)) {
                "만료된 카드입니다: ${expiryYearMonth}"
            }
        }

        private fun isValidLuhn(number: String): Boolean {
            var sum = 0
            var alternate = false

            for (i in number.length - 1 downTo 0) {
                var digit = number[i].digitToInt()

                if (alternate) {
                    digit *= 2
                    if (digit > 9) digit -= 9
                }

                sum += digit
                alternate = !alternate
            }

            return sum % 10 == 0
        }

        private fun maskCardNumber(cardNumber: String): String {
            val digitsOnly = cardNumber.replace(Regex("[^0-9]"), "")

            val first4 = digitsOnly.substring(0, 4)
            val last4 = digitsOnly.takeLast(4)
            val middle = digitsOnly.substring(4, digitsOnly.length - 4)
            val maskedMiddle = "*".repeat(middle.length)

            val masked = first4 + maskedMiddle + last4

            return masked.chunked(4).joinToString("-")
        }
    }
}

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
