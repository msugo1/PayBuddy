package com.paybuddy.payment.domain

import com.paybuddy.payment.domain.service.Bin
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.YearMonth

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
            bin: Bin
        ): Card {
            validateCardNumber(cardNumber)
            validateExpiry(expiryMonth, expiryYear)

            return Card(
                maskedNumber = maskCardNumber(cardNumber),
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                holderName = holderName,
                bin = bin.number,
                brand = bin.brand,
                issuerCode = bin.issuerCode,
                acquirerCode = bin.acquirerCode,
                cardType = bin.cardType,
                ownerType = bin.ownerType,
                issuedCountry = bin.issuedCountry,
                productCode = bin.productCode
            )
        }

        private fun validateCardNumber(cardNumber: String) {
            val digitsOnly = cardNumber.replace(Regex("[^0-9]"), "")

            require(digitsOnly.length in 13..19) { "유효하지 않은 카드 번호 길이입니다" }
            require(isValidLuhn(digitsOnly)) { "유효하지 않은 카드 번호입니다 (체크섬 오류)" }
        }

        private fun validateExpiry(month: Int, year: Int) {
            require(month in 1..12) { "유효하지 않은 만료 월입니다: $month" }
            require(year in 0..99 || year in 2000..2099) {
                "유효하지 않은 만료 연도 형식입니다: $year (0-99 또는 2000-2099만 허용)"
            }

            val now = YearMonth.now()
            val fullYear = if (year < 100) 2000 + year else year
            val expiryYearMonth = YearMonth.of(fullYear, month)

            require(!expiryYearMonth.isBefore(now)) {
                "만료된 카드입니다: $expiryYearMonth"
            }
        }

        /**
         * Luhn 알고리즘 (Luhn Algorithm, Modulo 10 검사)
         *
         * 카드 번호의 유효성을 검증하는 체크섬 알고리즘.
         * 주요 카드사(VISA, Mastercard, AMEX 등)의 카드 번호 표준으로 사용됨.
         *
         * 검증 방식:
         * 1. 오른쪽(끝)에서 왼쪽으로 순회하며, 짝수 번째 자리 숫자를 2배로 곱함
         * 2. 2배로 곱한 결과가 9보다 크면 9를 뺌 (또는 각 자릿수를 더함)
         * 3. 모든 숫자의 합이 10으로 나누어떨어지면 유효
         *
         * 예: 4111 1111 1111 1111
         * - 1*1 + 1*2 + 1*1 + 1*2 + ... = 20 (10으로 나누어떨어짐) → 유효
         */
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

            return first4 + maskedMiddle + last4
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
