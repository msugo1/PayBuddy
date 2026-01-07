package com.paybuddy.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CardTest {
    private val binLookupService = FakeBinLookupService()

    @Test
    fun `카드 생성 - 모든 필드 매핑 검증`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, "HONG GILDONG", bin)

        // Then
        assertThat(card).isEqualTo(
            Card(
                maskedNumber = "4111-****-****-1111",
                expiryMonth = 12,
                expiryYear = 28,
                holderName = "HONG GILDONG",
                bin = "411111",
                brand = CardBrand.VISA,
                issuerCode = "04",
                acquirerCode = "04",
                cardType = CardType.CREDIT,
                ownerType = OwnerType.PERSONAL,
                issuedCountry = "KR",
                productCode = "VISA-CLASSIC"
            )
        )
    }

    @Test
    fun `카드 번호 마스킹 - 16자리`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.maskedNumber).isEqualTo("4111-****-****-1111")
    }

    @Test
    fun `카드 번호 마스킹 - 15자리 AMEX`() {
        // Given
        val cardNumber = "340000000000009"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.maskedNumber).isEqualTo("3400-****-***0-009")
    }

    @Test
    fun `카드 번호 마스킹 - 하이픈 포함된 입력`() {
        // Given
        val cardNumber = "4111-1111-1111-1111"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.maskedNumber).isEqualTo("4111-****-****-1111")
    }

    @Test
    fun `카드 번호 마스킹 - 공백 포함된 입력`() {
        // Given
        val cardNumber = "4111 1111 1111 1111"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.maskedNumber).isEqualTo("4111-****-****-1111")
    }

    @Test
    fun `Bin 정보 매핑 - MASTERCARD DEBIT`() {
        // Given
        val cardNumber = "5100000000000008"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.brand).isEqualTo(CardBrand.MASTERCARD)
        assertThat(card.issuerCode).isEqualTo("06")
        assertThat(card.acquirerCode).isEqualTo("06")
        assertThat(card.cardType).isEqualTo(CardType.DEBIT)
        assertThat(card.ownerType).isEqualTo(OwnerType.CORPORATE)
        assertThat(card.issuedCountry).isEqualTo("US")
        assertThat(card.productCode).isNull()
    }

    @Test
    fun `만료일 검증 - 유효한 월`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.expiryMonth).isEqualTo(12)
        assertThat(card.expiryYear).isEqualTo(28)
    }

    @Test
    fun `만료일 검증 실패 - 유효하지 않은 월 (0)`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, 0, 25, null, bin)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("유효하지 않은 만료 월")
    }

    @Test
    fun `만료일 검증 실패 - 유효하지 않은 월 (13)`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, 13, 25, null, bin)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("유효하지 않은 만료 월")
    }

    @Test
    fun `만료일 검증 실패 - 음수 연도`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, 12, -1, null, bin)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("유효하지 않은 만료 연도")
    }

    @Test
    fun `만료일 검증 실패 - 이미 만료된 카드 (2자리 년도)`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, 12, 20, null, bin)  // 2020년 12월
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("만료된 카드")
    }

    @Test
    fun `만료일 검증 실패 - 이미 만료된 카드 (4자리 년도)`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, 1, 2020, null, bin)  // 2020년 1월
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("만료된 카드")
    }

    @Test
    fun `만료일 검증 성공 - 현재 월`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)
        val now = java.time.YearMonth.now()

        // When
        val card = Card.create(cardNumber, now.monthValue, now.year % 100, null, bin)

        // Then
        assertThat(card.expiryMonth).isEqualTo(now.monthValue)
        assertThat(card.expiryYear).isEqualTo(now.year % 100)
    }

    @Test
    fun `만료일 검증 성공 - 미래 날짜`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 30, null, bin)  // 2030년 12월

        // Then
        assertThat(card.expiryMonth).isEqualTo(12)
        assertThat(card.expiryYear).isEqualTo(30)
    }

    @Test
    fun `카드 소유자 이름 - 있음`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, "HONG GILDONG", bin)

        // Then
        assertThat(card.holderName).isEqualTo("HONG GILDONG")
    }

    @Test
    fun `카드 소유자 이름 - 없음 (null)`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.holderName).isNull()
    }

    @Test
    fun `유효하지 않은 카드 번호 길이 - 12자리`() {
        // Given
        val cardNumber = "123456789012"
        val bin = binLookupService.lookup(cardNumber)

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, 12, 28, null, bin)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("유효하지 않은 카드 번호 길이")
    }

    @Test
    fun `유효하지 않은 카드 번호 길이 - 20자리`() {
        // Given
        val cardNumber = "12345678901234567890"
        val bin = binLookupService.lookup(cardNumber)

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, 12, 28, null, bin)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("유효하지 않은 카드 번호 길이")
    }

    @Test
    fun `Luhn 체크섬 검증 실패`() {
        // Given
        val cardNumber = "4111111111111112"
        val bin = binLookupService.lookup(cardNumber)

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, 12, 28, null, bin)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("체크섬 오류")
    }

    @Test
    fun `Luhn 체크섬 검증 성공 - VISA`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.brand).isEqualTo(CardBrand.VISA)
        assertThat(card.bin).isEqualTo("411111")
    }

    @Test
    fun `다양한 카드 브랜드 - VISA`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.brand).isEqualTo(CardBrand.VISA)
        assertThat(card.issuerCode).isEqualTo("04")
        assertThat(card.issuedCountry).isEqualTo("KR")
        assertThat(card.productCode).isEqualTo("VISA-CLASSIC")
    }

    @Test
    fun `다양한 카드 브랜드 - MASTERCARD`() {
        // Given
        val cardNumber = "5500000000000004"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.brand).isEqualTo(CardBrand.MASTERCARD)
        assertThat(card.issuerCode).isEqualTo("06")
        assertThat(card.issuedCountry).isEqualTo("KR")
        assertThat(card.productCode).isEqualTo("MC-STANDARD")
    }

    @Test
    fun `다양한 카드 브랜드 - AMEX`() {
        // Given
        val cardNumber = "340000000000009"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.brand).isEqualTo(CardBrand.AMEX)
        assertThat(card.issuerCode).isEqualTo("03")
        assertThat(card.issuedCountry).isEqualTo("US")
        assertThat(card.productCode).isEqualTo("AMEX-GOLD")
        assertThat(card.maskedNumber).isEqualTo("3400-****-***0-009")
    }

    @Test
    fun `다양한 카드 브랜드 - JCB`() {
        // Given
        val cardNumber = "3566002020360505"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.brand).isEqualTo(CardBrand.JCB)
        assertThat(card.issuerCode).isEqualTo("07")
        assertThat(card.issuedCountry).isEqualTo("JP")
        assertThat(card.productCode).isEqualTo("JCB-STANDARD")
    }

    @Test
    fun `카드 타입 - DEBIT 체크카드`() {
        // Given
        val cardNumber = "4000000000000002"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.cardType).isEqualTo(CardType.DEBIT)
        assertThat(card.productCode).isEqualTo("VISA-DEBIT")
    }

    @Test
    fun `카드 타입 - PREPAID 선불카드`() {
        // Given
        val cardNumber = "4000000000000028"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.cardType).isEqualTo(CardType.PREPAID)
        assertThat(card.productCode).isEqualTo("VISA-PREPAID")
    }

    @Test
    fun `소유자 타입 - 법인카드`() {
        // Given
        val cardNumber = "4000000000000051"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.ownerType).isEqualTo(OwnerType.CORPORATE)
        assertThat(card.productCode).isEqualTo("VISA-CORPORATE")
    }

    @Test
    fun `해외 발급 카드 - 미국`() {
        // Given
        val cardNumber = "4000000000000077"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.issuedCountry).isEqualTo("US")
        assertThat(card.productCode).isEqualTo("VISA-US")
    }

    @Test
    fun `해외 발급 카드 - 일본`() {
        // Given
        val cardNumber = "3566002020360505"
        val bin = binLookupService.lookup(cardNumber)

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.issuedCountry).isEqualTo("JP")
        assertThat(card.brand).isEqualTo(CardBrand.JCB)
    }
}

class FakeBinLookupService : BinLookupService {
    override fun lookup(cardNumber: String): Bin {
        val digitsOnly = cardNumber.replace(Regex("[^0-9]"), "")
        val binNumber = digitsOnly.take(6)

        return when (digitsOnly) {
            // VISA
            "4111111111111111" -> visaCredit(binNumber)
            "4000000000000002" -> visaDebit(binNumber)
            "4000000000000028" -> visaPrepaid(binNumber)
            "4000000000000051" -> visaCorporate(binNumber)
            "4000000000000077" -> visaUs(binNumber)

            // Mastercard
            "5500000000000004" -> mastercardCredit(binNumber)
            "5100000000000008" -> mastercardDebit(binNumber)

            // AMEX
            "340000000000009" -> amexCredit(binNumber)

            // JCB
            "3566002020360505" -> jcbCredit(binNumber)

            // Default fallback by prefix
            else -> when {
                binNumber.startsWith("4") -> visaCredit(binNumber)
                binNumber.startsWith("51") || binNumber.startsWith("52") || binNumber.startsWith("53") ||
                binNumber.startsWith("54") || binNumber.startsWith("55") -> mastercardCredit(binNumber)
                binNumber.startsWith("34") || binNumber.startsWith("37") -> amexCredit(binNumber)
                binNumber.startsWith("35") -> jcbCredit(binNumber)
                else -> localCard(binNumber)
            }
        }
    }

    private fun visaCredit(binNumber: String) = Bin(
        number = binNumber,
        brand = CardBrand.VISA,
        issuerCode = "04",
        acquirerCode = "04",
        cardType = CardType.CREDIT,
        ownerType = OwnerType.PERSONAL,
        issuedCountry = "KR",
        productCode = "VISA-CLASSIC"
    )

    private fun visaDebit(binNumber: String) = Bin(
        number = binNumber,
        brand = CardBrand.VISA,
        issuerCode = "04",
        acquirerCode = "04",
        cardType = CardType.DEBIT,
        ownerType = OwnerType.PERSONAL,
        issuedCountry = "KR",
        productCode = "VISA-DEBIT"
    )

    private fun visaPrepaid(binNumber: String) = Bin(
        number = binNumber,
        brand = CardBrand.VISA,
        issuerCode = "04",
        acquirerCode = "04",
        cardType = CardType.PREPAID,
        ownerType = OwnerType.PERSONAL,
        issuedCountry = "KR",
        productCode = "VISA-PREPAID"
    )

    private fun visaCorporate(binNumber: String) = Bin(
        number = binNumber,
        brand = CardBrand.VISA,
        issuerCode = "04",
        acquirerCode = "04",
        cardType = CardType.CREDIT,
        ownerType = OwnerType.CORPORATE,
        issuedCountry = "KR",
        productCode = "VISA-CORPORATE"
    )

    private fun visaUs(binNumber: String) = Bin(
        number = binNumber,
        brand = CardBrand.VISA,
        issuerCode = "99",
        acquirerCode = "99",
        cardType = CardType.CREDIT,
        ownerType = OwnerType.PERSONAL,
        issuedCountry = "US",
        productCode = "VISA-US"
    )

    private fun mastercardCredit(binNumber: String) = Bin(
        number = binNumber,
        brand = CardBrand.MASTERCARD,
        issuerCode = "06",
        acquirerCode = "06",
        cardType = CardType.CREDIT,
        ownerType = OwnerType.PERSONAL,
        issuedCountry = "KR",
        productCode = "MC-STANDARD"
    )

    private fun mastercardDebit(binNumber: String) = Bin(
        number = binNumber,
        brand = CardBrand.MASTERCARD,
        issuerCode = "06",
        acquirerCode = "06",
        cardType = CardType.DEBIT,
        ownerType = OwnerType.CORPORATE,
        issuedCountry = "US",
        productCode = null
    )

    private fun amexCredit(binNumber: String) = Bin(
        number = binNumber,
        brand = CardBrand.AMEX,
        issuerCode = "03",
        acquirerCode = "03",
        cardType = CardType.CREDIT,
        ownerType = OwnerType.PERSONAL,
        issuedCountry = "US",
        productCode = "AMEX-GOLD"
    )

    private fun jcbCredit(binNumber: String) = Bin(
        number = binNumber,
        brand = CardBrand.JCB,
        issuerCode = "07",
        acquirerCode = "07",
        cardType = CardType.CREDIT,
        ownerType = OwnerType.PERSONAL,
        issuedCountry = "JP",
        productCode = "JCB-STANDARD"
    )

    private fun localCard(binNumber: String) = Bin(
        number = binNumber,
        brand = CardBrand.LOCAL,
        issuerCode = "99",
        acquirerCode = "99",
        cardType = CardType.CREDIT,
        ownerType = OwnerType.PERSONAL,
        issuedCountry = "KR",
        productCode = null
    )
}
