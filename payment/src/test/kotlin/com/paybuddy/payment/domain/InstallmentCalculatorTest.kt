package com.paybuddy.payment.domain

import com.paybuddy.payment.domain.installment.*
import com.paybuddy.payment.domain.merchant.MerchantInstallmentPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class InstallmentCalculatorTest {

    private val sut = InstallmentCalculator()

    @ParameterizedTest
    @EnumSource(value = CardType::class, names = ["DEBIT", "PREPAID"])
    fun `신용카드가 아니면 할부가 불가능하다`(cardType: CardType) {
        // Given
        val merchantPolicy = createMerchantPolicy(supportsInstallment = true, minInstallmentAmount = 50_000, availableMonths = setOf(2, 3, 6, 12))
        val issuerPolicy = createIssuerPolicy(availableMonths = setOf(2, 3, 6), interestFreeMonths = setOf(2, 3))

        // When
        val options = sut.calculateAvailableOptions(merchantPolicy, issuerPolicy, cardType, 100_000)

        // Then
        assertThat(options).isEqualTo(InstallmentOptions.UNAVAILABLE)
    }

    @Test
    fun `가맹점이 할부를 지원하지 않으면 할부가 불가능하다`() {
        // Given
        val merchantPolicy = createMerchantPolicy(supportsInstallment = false, minInstallmentAmount = 50_000, availableMonths = emptySet())
        val issuerPolicy = createIssuerPolicy(availableMonths = setOf(2, 3, 6), interestFreeMonths = setOf(2, 3))

        // When
        val options = sut.calculateAvailableOptions(merchantPolicy, issuerPolicy, CardType.CREDIT, 100_000)

        // Then
        assertThat(options).isEqualTo(InstallmentOptions.UNAVAILABLE)
    }

    @Test
    fun `결제 금액이 최소 금액보다 작으면 할부가 불가능하다`() {
        // Given
        val merchantPolicy = createMerchantPolicy(supportsInstallment = true, minInstallmentAmount = 50_000, availableMonths = setOf(2, 3, 6, 12))
        val issuerPolicy = createIssuerPolicy(availableMonths = setOf(2, 3, 6), interestFreeMonths = setOf(2, 3))

        // When
        val options = sut.calculateAvailableOptions(merchantPolicy, issuerPolicy, CardType.CREDIT, 49_999)

        // Then
        assertThat(options).isEqualTo(InstallmentOptions.UNAVAILABLE)
    }

    @Test
    fun `결제 금액이 최소 금액과 같으면 할부가 가능하다`() {
        // Given
        val merchantPolicy = createMerchantPolicy(supportsInstallment = true, minInstallmentAmount = 50_000, availableMonths = setOf(2, 3, 6, 12))
        val issuerPolicy = createIssuerPolicy(availableMonths = setOf(2, 3, 6, 12), interestFreeMonths = setOf(2, 3))

        // When
        val options = sut.calculateAvailableOptions(merchantPolicy, issuerPolicy, CardType.CREDIT, 50_000)

        // Then
        assertThat(options).isEqualTo(
            InstallmentOptions(
                supported = true,
                availableMonths = setOf(2, 3, 6, 12),
                interestFreeMonths = setOf(2, 3)
            )
        )
    }

    @Test
    fun `카드사가 할부를 지원하지 않으면 할부가 불가능하다`() {
        // Given
        val merchantPolicy = createMerchantPolicy(supportsInstallment = true, minInstallmentAmount = 50_000, availableMonths = setOf(2, 3, 6, 12))
        val issuerPolicy = createIssuerPolicy(availableMonths = emptySet(), interestFreeMonths = emptySet())

        // When
        val options = sut.calculateAvailableOptions(merchantPolicy, issuerPolicy, CardType.CREDIT, 100_000)

        // Then
        assertThat(options).isEqualTo(InstallmentOptions.UNAVAILABLE)
    }

    @Test
    fun `가맹점과 카드사가 공통으로 지원하는 할부 개월이 없으면 할부가 지원되지 않는다`() {
        // Given
        val merchantPolicy = createMerchantPolicy(supportsInstallment = true, minInstallmentAmount = 50_000, availableMonths = setOf(2, 3, 6))
        val issuerPolicy = createIssuerPolicy(
            availableMonths = setOf(10, 12),
            interestFreeMonths = emptySet()
        )

        // When
        val options = sut.calculateAvailableOptions(merchantPolicy, issuerPolicy, CardType.CREDIT, 100_000)

        // Then
        assertThat(options).isEqualTo(InstallmentOptions.UNAVAILABLE)
    }

    @Test
    fun `가맹점 또는 카드사가 공통으로 지원하는 할부 개월만 할부 옵션에 포함된다`() {
        // Given
        val merchantPolicy = createMerchantPolicy(supportsInstallment = true, minInstallmentAmount = 50_000, availableMonths = setOf(2, 3, 4, 5, 6))
        val issuerPolicy = createIssuerPolicy(
            availableMonths = setOf(2, 3, 4, 5, 6, 10, 12),
            interestFreeMonths = setOf(2, 3)
        )

        // When
        val options = sut.calculateAvailableOptions(merchantPolicy, issuerPolicy, CardType.CREDIT, 100_000)

        // Then
        assertThat(options).isEqualTo(
            InstallmentOptions(
                supported = true,
                availableMonths = setOf(2, 3, 4, 5, 6),
                interestFreeMonths = setOf(2, 3)
            )
        )
    }

    @Test
    fun `신용카드이고 조건을 만족하면 무이자 할부가 가능하다`() {
        // Given
        val merchantPolicy = createMerchantPolicy(supportsInstallment = true, minInstallmentAmount = 50_000, availableMonths = setOf(2, 3, 6, 12))
        val issuerPolicy = createIssuerPolicy(
            availableMonths = setOf(2, 3, 6, 12),
            interestFreeMonths = setOf(2, 3)
        )

        // When
        val options = sut.calculateAvailableOptions(merchantPolicy, issuerPolicy, CardType.CREDIT, 100_000)

        // Then
        assertThat(options).isEqualTo(
            InstallmentOptions(
                supported = true,
                availableMonths = setOf(2, 3, 6, 12),
                interestFreeMonths = setOf(2, 3)
            )
        )
    }

    @Test
    fun `발급사에서 무이자를 지원하지 않으면 무이자 할부는 불가능하다`() {
        // Given
        val merchantPolicy = createMerchantPolicy(supportsInstallment = true, minInstallmentAmount = 50_000, availableMonths = setOf(2, 3, 6, 12))
        val issuerPolicy = createIssuerPolicy(
            availableMonths = setOf(2, 3, 6, 12),
            interestFreeMonths = emptySet()
        )

        // When
        val options = sut.calculateAvailableOptions(merchantPolicy, issuerPolicy, CardType.CREDIT, 100_000)

        // Then
        assertThat(options).isEqualTo(
            InstallmentOptions(
                supported = true,
                availableMonths = setOf(2, 3, 6, 12),
                interestFreeMonths = emptySet()
            )
        )
    }

    @Test
    fun `발급사 최소 금액보다 작으면 할부가 불가능하다`() {
        // Given
        val merchantPolicy = createMerchantPolicy(supportsInstallment = true, minInstallmentAmount = 50_000, availableMonths = setOf(2, 3, 6, 12))
        val issuerPolicy = createIssuerPolicy(
            availableMonths = setOf(2, 3, 6, 12),
            interestFreeMonths = setOf(2, 3),
            minInstallmentAmount = 100_000
        )

        // When
        val options = sut.calculateAvailableOptions(merchantPolicy, issuerPolicy, CardType.CREDIT, 80_000)

        // Then
        assertThat(options).isEqualTo(InstallmentOptions.UNAVAILABLE)
    }

    @Test
    fun `가맹점과 발급사 중 더 큰 최소 금액을 적용한다`() {
        // Given
        val merchantPolicy = createMerchantPolicy(supportsInstallment = true, minInstallmentAmount = 50_000, availableMonths = setOf(2, 3, 6, 12))
        val issuerPolicy = createIssuerPolicy(
            availableMonths = setOf(2, 3, 6, 12),
            interestFreeMonths = setOf(2, 3),
            minInstallmentAmount = 100_000  // 가맹점(50,000)보다 큼
        )

        // When
        val options = sut.calculateAvailableOptions(merchantPolicy, issuerPolicy, CardType.CREDIT, 100_000)

        // Then
        assertThat(options).isEqualTo(
            InstallmentOptions(
                supported = true,
                availableMonths = setOf(2, 3, 6, 12),
                interestFreeMonths = setOf(2, 3)
            )
        )
    }

    private fun createMerchantPolicy(
        supportsInstallment: Boolean,
        minInstallmentAmount: Long,
        availableMonths: Set<Int>
    ): MerchantInstallmentPolicy {
        return MerchantInstallmentPolicy(
            merchantId = "mch_123",
            supportsInstallment = supportsInstallment,
            minInstallmentAmount = minInstallmentAmount,
            availableMonths = availableMonths
        )
    }

    private fun createIssuerPolicy(
        availableMonths: Set<Int>,
        interestFreeMonths: Set<Int>,
        minInstallmentAmount: Long = 0
    ): IssuerInstallmentPolicy {
        return IssuerInstallmentPolicy(
            issuerCode = "04",
            availableMonths = availableMonths,
            interestFreeMonths = interestFreeMonths,
            minInstallmentAmount = minInstallmentAmount
        )
    }
}
