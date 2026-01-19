package com.paybuddy.payment.domain

import com.paybuddy.payment.domain.merchant.*
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MerchantContractValidatorTest {
    private val validator = MerchantContractValidator()
    private val minPaymentAmount = 1_000L

    @Test
    fun `정상 가맹점은 검증 통과`() {
        // Given
        val merchantId = "merchant-active"
        val contract = MerchantContract(
            merchantId = merchantId,
            status = MerchantStatus.ACTIVE,
            contractEndDate = LocalDate.now().plusYears(1),
            mcc = "5411",
            paymentMethodPolicies = mapOf(
                PaymentMethodType.CARD to PaymentMethodPolicy(enabled = true)
            ),
            installmentPolicy = MerchantInstallmentPolicy(
                merchantId = merchantId,
                supportsInstallment = false,
                minInstallmentAmount = 0,
                availableMonths = emptySet()
            )
        )

        // When & Then
        assertThatCode {
            validator.validate(contract, PaymentMethodType.CARD, 10_000L, minPaymentAmount)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `정지된 가맹점은 검증 실패`() {
        // Given
        val merchantId = "merchant-suspended"
        val contract = MerchantContract(
            merchantId = merchantId,
            status = MerchantStatus.SUSPENDED,
            contractEndDate = LocalDate.now().plusYears(1),
            mcc = "5411",
            paymentMethodPolicies = mapOf(
                PaymentMethodType.CARD to PaymentMethodPolicy(enabled = true)
            ),
            installmentPolicy = MerchantInstallmentPolicy(
                merchantId = merchantId,
                supportsInstallment = false,
                minInstallmentAmount = 0,
                availableMonths = emptySet()
            )
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(contract, PaymentMethodType.CARD, 10_000L, minPaymentAmount)
        }.isInstanceOf(MerchantSuspendedException::class.java)
    }

    @Test
    fun `해지된 가맹점은 검증 실패`() {
        // Given
        val merchantId = "merchant-terminated"
        val contract = MerchantContract(
            merchantId = merchantId,
            status = MerchantStatus.TERMINATED,
            contractEndDate = LocalDate.now().plusYears(1),
            mcc = "5411",
            paymentMethodPolicies = mapOf(
                PaymentMethodType.CARD to PaymentMethodPolicy(enabled = true)
            ),
            installmentPolicy = MerchantInstallmentPolicy(
                merchantId = merchantId,
                supportsInstallment = false,
                minInstallmentAmount = 0,
                availableMonths = emptySet()
            )
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(contract, PaymentMethodType.CARD, 10_000L, minPaymentAmount)
        }.isInstanceOf(MerchantTerminatedException::class.java)
    }

    @Test
    fun `계약 만료된 가맹점은 검증 실패`() {
        // Given
        val merchantId = "merchant-expired"
        val contract = MerchantContract(
            merchantId = merchantId,
            status = MerchantStatus.ACTIVE,
            contractEndDate = LocalDate.now().minusDays(1),
            mcc = "5411",
            paymentMethodPolicies = mapOf(
                PaymentMethodType.CARD to PaymentMethodPolicy(enabled = true)
            ),
            installmentPolicy = MerchantInstallmentPolicy(
                merchantId = merchantId,
                supportsInstallment = false,
                minInstallmentAmount = 0,
                availableMonths = emptySet()
            )
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(contract, PaymentMethodType.CARD, 10_000L, minPaymentAmount)
        }.isInstanceOf(ContractExpiredException::class.java)
    }

    @Test
    fun `허용되지 않은 결제수단은 검증 실패`() {
        // Given
        val merchantId = "merchant-no-card"
        val contract = MerchantContract(
            merchantId = merchantId,
            status = MerchantStatus.ACTIVE,
            contractEndDate = LocalDate.now().plusYears(1),
            mcc = "5411",
            paymentMethodPolicies = mapOf(
                PaymentMethodType.VIRTUAL_ACCOUNT to PaymentMethodPolicy(enabled = true)
            ),
            installmentPolicy = MerchantInstallmentPolicy(
                merchantId = merchantId,
                supportsInstallment = false,
                minInstallmentAmount = 0,
                availableMonths = emptySet()
            )
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(contract, PaymentMethodType.CARD, 10_000L, minPaymentAmount)
        }.isInstanceOf(PaymentMethodNotAllowedException::class.java)
    }

    @Test
    fun `비활성화된 결제수단은 검증 실패`() {
        // Given
        val merchantId = "merchant-card-disabled"
        val contract = MerchantContract(
            merchantId = merchantId,
            status = MerchantStatus.ACTIVE,
            contractEndDate = LocalDate.now().plusYears(1),
            mcc = "5411",
            paymentMethodPolicies = mapOf(
                PaymentMethodType.CARD to PaymentMethodPolicy(enabled = false)
            ),
            installmentPolicy = MerchantInstallmentPolicy(
                merchantId = merchantId,
                supportsInstallment = false,
                minInstallmentAmount = 0,
                availableMonths = emptySet()
            )
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(contract, PaymentMethodType.CARD, 10_000L, minPaymentAmount)
        }.isInstanceOf(PaymentMethodNotAllowedException::class.java)
    }

    @Test
    fun `전역 최소금액 미만은 검증 실패`() {
        // Given
        val merchantId = "merchant-active"
        val contract = MerchantContract(
            merchantId = merchantId,
            status = MerchantStatus.ACTIVE,
            contractEndDate = LocalDate.now().plusYears(1),
            mcc = "5411",
            paymentMethodPolicies = mapOf(
                PaymentMethodType.CARD to PaymentMethodPolicy(enabled = true)
            ),
            installmentPolicy = MerchantInstallmentPolicy(
                merchantId = merchantId,
                supportsInstallment = false,
                minInstallmentAmount = 0,
                availableMonths = emptySet()
            )
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(contract, PaymentMethodType.CARD, 500L, minPaymentAmount)
        }.isInstanceOf(AmountBelowMinimumException::class.java)
    }

    @Test
    fun `가맹점별 최소금액 미만은 검증 실패`() {
        // Given
        val merchantId = "merchant-high-min"
        val contract = MerchantContract(
            merchantId = merchantId,
            status = MerchantStatus.ACTIVE,
            contractEndDate = LocalDate.now().plusYears(1),
            mcc = "5411",
            paymentMethodPolicies = mapOf(
                PaymentMethodType.CARD to PaymentMethodPolicy(
                    enabled = true,
                    minAmount = 50_000L
                )
            ),
            installmentPolicy = MerchantInstallmentPolicy(
                merchantId = merchantId,
                supportsInstallment = false,
                minInstallmentAmount = 0,
                availableMonths = emptySet()
            )
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(contract, PaymentMethodType.CARD, 10_000L, minPaymentAmount)
        }.isInstanceOf(AmountBelowMinimumException::class.java)
    }

}
