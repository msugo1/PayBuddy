package com.paybuddy.payment.domain

import com.paybuddy.payment.domain.service.Bin
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class CardTest {
    
    private val BIN_SAMPLE = Bin(
        number = "411111",
        brand = CardBrand.VISA,
        issuerCode = "04",
        acquirerCode = "04",
        cardType = CardType.CREDIT,
        ownerType = OwnerType.PERSONAL,
        issuedCountry = "KR",
        productCode = "VISA-CLASSIC"
    )

    @Test
    fun `카드 생성 시 모든 필드가 정확히 매핑된다`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = BIN_SAMPLE

        // When
        val card = Card.create(cardNumber, 12, 28, "HONG GILDONG", bin)

        // Then
        assertThat(card).isEqualTo(
            Card(
                maskedNumber = "4111********1111",
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
    fun `소유자 이름이 없어도 카드를 생성할 수 있다`() {
        // Given
        val cardNumber = "4111111111111111"
        val bin = BIN_SAMPLE

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.holderName).isNull()
    }

    // ===== 카드 번호 마스킹 =====

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
        "4111111111111111, 4111********1111",       // 16자리 기본
        "4111-1111-1111-1111, 4111********1111",   // 하이픈 제거 후 마스킹
        "4111 1111 1111 1111, 4111********1111",   // 공백 제거 후 마스킹
        "5555555555554444, 5555********4444",       // 16자리 다른 번호
        "378282246310005, 3782*******0005",         // 15자리 (AMEX)
        "4222222222222, 4222*****2222",             // 13자리 (최소)
        "6304000000000000000, 6304***********0000"  // 19자리 (최대)
    )
    fun `카드 번호는 앞 4자리와 뒤 4자리만 노출하고 나머지는 마스킹된다`(input: String, expected: String) {
        // Given
        val bin = BIN_SAMPLE

        // When
        val card = Card.create(input, 12, 28, null, bin)

        // Then
        assertThat(card.maskedNumber).isEqualTo(expected)
    }

    // ===== 만료일 검증 =====

    @ParameterizedTest(name = "월: {0}")
    @ValueSource(ints = [1, 6, 12])
    fun `만료 월은 1-12 사이만 허용한다`(month: Int) {
        // Given
        val cardNumber = "4111111111111111"
        val bin = BIN_SAMPLE

        // When
        val card = Card.create(cardNumber, month, 28, null, bin)

        // Then
        assertThat(card.expiryMonth).isEqualTo(month)
    }

    @ParameterizedTest(name = "월: {0}")
    @ValueSource(ints = [0, 13, -1, 100])
    fun `만료 월이 1-12 범위를 벗어나면 카드를 생성할 수 없다`(month: Int) {
        // Given
        val cardNumber = "4111111111111111"
        val bin = BIN_SAMPLE

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, month, 28, null, bin)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("유효하지 않은 만료 월")
    }

    @ParameterizedTest(name = "년: {0}")
    @CsvSource(
        "26",    // 2026년 (2자리, 현재 기준)
        "27",    // 2027년
        "50",    // 2050년
        "99",    // 2099년 (2자리 최대)
        "2026",  // 2026년 (4자리, 현재 기준)
        "2027",  // 2027년
        "2050",  // 2050년
        "2099"   // 2099년 (4자리 최대)
    )
    fun `만료 년도는 2자리(0-99) 또는 4자리(2000-2099) 형식만 허용한다`(year: Int) {
        // Given
        val cardNumber = "4111111111111111"
        val bin = BIN_SAMPLE

        // When
        val card = Card.create(cardNumber, 12, year, null, bin)

        // Then
        assertThat(card.expiryYear).isEqualTo(year)
    }

    @ParameterizedTest(name = "년: {0}")
    @CsvSource(
        "-1",    // 음수
        "100",   // 3자리 (100-1999 불허)
        "101",
        "1999",
        "2100",  // 2100년 이상 불허
        "3000"
    )
    fun `만료 년도 형식이 올바르지 않으면 카드를 생성할 수 없다`(year: Int) {
        // Given
        val cardNumber = "4111111111111111"
        val bin = BIN_SAMPLE

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, 12, year, null, bin)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("유효하지 않은 만료 연도 형식")
    }

    @ParameterizedTest(name = "{0}년 {1}월")
    @CsvSource(
        "12, 20",    // 2020년 12월
        "1, 2020",   // 2020년 1월 (4자리)
        "6, 24"      // 2024년 6월
    )
    fun `만료 기한이 지난 카드는 생성할 수 없다`(month: Int, year: Int) {
        // Given
        val cardNumber = "4111111111111111"
        val bin = BIN_SAMPLE

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, month, year, null, bin)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("만료된 카드")
    }

    // ===== Luhn 알고리즘 검증 =====

    @ParameterizedTest(name = "유효: {0}")
    @ValueSource(
        strings = [
            "4222222222222",      // 13자리 (최소)
            "378282246310005",    // 15자리 (AMEX)
            "4111111111111111",   // 16자리 (VISA)
            "5555555555554444",   // 16자리 (Mastercard)
            "6011111111111117",   // 16자리 (Discover)
            "3530111333300000",   // 16자리 (JCB)
            "6304000000000000000" // 19자리 (최대)
        ]
    )
    fun `Luhn 알고리즘을 통과한 카드 번호만 허용한다`(cardNumber: String) {
        // Given
        val bin = BIN_SAMPLE

        // When
        val card = Card.create(cardNumber, 12, 28, null, bin)

        // Then
        assertThat(card.maskedNumber).isNotEmpty()
    }

    @ParameterizedTest(name = "무효: {0}")
    @ValueSource(
        strings = [
            "4222222222223",      // 13자리 체크섬 오류 (마지막 자리 +1)
            "4111111111111112",   // 16자리 마지막 자리 +1 (체크 디지트 오류)
            "4111111111111110",   // 16자리 마지막 자리 -1
            "5555555555554445",   // 16자리 체크섬 오류
            "5555555555554443",   // 16자리 체크섬 오류
            "6304000000000000001", // 19자리 체크섬 오류 (마지막 자리 +1)
            "1234567890123456",   // 무작위 번호
            "1111111111111111"    // 모두 같은 숫자
        ]
    )
    fun `Luhn 알고리즘 체크섬이 맞지 않는 카드는 허용하지 않는다`(cardNumber: String) {
        // Given
        val bin = BIN_SAMPLE

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, 12, 28, null, bin)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("체크섬 오류")
    }

    @ParameterizedTest(name = "{0}자리")
    @ValueSource(
        strings = [
            "123456789012",           // 12자리 (최소 미만)
            "12345678901234567890"    // 20자리 (최대 초과)
        ]
    )
    fun `카드 번호는 13-19자리만 허용한다`(cardNumber: String) {
        // Given
        val bin = BIN_SAMPLE

        // When & Then
        assertThatThrownBy {
            Card.create(cardNumber, 12, 28, null, bin)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("유효하지 않은 카드 번호 길이")
    }
}
