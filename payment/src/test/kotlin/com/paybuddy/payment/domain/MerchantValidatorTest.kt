package com.paybuddy.payment.domain

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MerchantValidatorTest {
    private val merchantContractService = FakeMerchantContractService()
    private val merchantLimitService = FakeMerchantLimitService()
    private val paymentPolicy = DefaultPaymentPolicy()
    private val validator = MerchantValidator(
        merchantContractService,
        merchantLimitService,
        paymentPolicy
    )

    @Test
    fun `정상 가맹점은 검증 통과`() {
        // Given
        val merchantId = "merchant-active"
        merchantContractService.setContract(
            MerchantContract(
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
        )

        // When & Then
        assertThatCode {
            validator.validate(merchantId, PaymentMethodType.CARD, 10_000L)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `정지된 가맹점은 검증 실패`() {
        // Given
        val merchantId = "merchant-suspended"
        merchantContractService.setContract(
            MerchantContract(
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
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(merchantId, PaymentMethodType.CARD, 10_000L)
        }.isInstanceOf(MerchantSuspendedException::class.java)
    }

    @Test
    fun `해지된 가맹점은 검증 실패`() {
        // Given
        val merchantId = "merchant-terminated"
        merchantContractService.setContract(
            MerchantContract(
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
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(merchantId, PaymentMethodType.CARD, 10_000L)
        }.isInstanceOf(MerchantTerminatedException::class.java)
    }

    @Test
    fun `계약 만료된 가맹점은 검증 실패`() {
        // Given
        val merchantId = "merchant-expired"
        merchantContractService.setContract(
            MerchantContract(
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
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(merchantId, PaymentMethodType.CARD, 10_000L)
        }.isInstanceOf(ContractExpiredException::class.java)
    }

    @Test
    fun `허용되지 않은 결제수단은 검증 실패`() {
        // Given
        val merchantId = "merchant-no-card"
        merchantContractService.setContract(
            MerchantContract(
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
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(merchantId, PaymentMethodType.CARD, 10_000L)
        }.isInstanceOf(PaymentMethodNotAllowedException::class.java)
    }

    @Test
    fun `비활성화된 결제수단은 검증 실패`() {
        // Given
        val merchantId = "merchant-card-disabled"
        merchantContractService.setContract(
            MerchantContract(
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
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(merchantId, PaymentMethodType.CARD, 10_000L)
        }.isInstanceOf(PaymentMethodNotAllowedException::class.java)
    }

    @Test
    fun `전역 최소금액 미만은 검증 실패`() {
        // Given
        val merchantId = "merchant-active"
        merchantContractService.setContract(
            MerchantContract(
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
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(merchantId, PaymentMethodType.CARD, 500L)
        }.isInstanceOf(AmountBelowMinimumException::class.java)
    }

    @Test
    fun `가맹점별 최소금액 미만은 검증 실패`() {
        // Given
        val merchantId = "merchant-high-min"
        merchantContractService.setContract(
            MerchantContract(
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
        )

        // When & Then
        assertThatThrownBy {
            validator.validate(merchantId, PaymentMethodType.CARD, 10_000L)
        }.isInstanceOf(AmountBelowMinimumException::class.java)
    }

    @Test
    fun `가맹점 한도 초과는 검증 실패`() {
        // Given
        val merchantId = "merchant-limit"
        merchantContractService.setContract(
            MerchantContract(
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
        )
        merchantLimitService.setLimit(merchantId, 100_000L)

        // When & Then
        assertThatThrownBy {
            validator.validate(merchantId, PaymentMethodType.CARD, 200_000L)
        }.isInstanceOf(MerchantLimitExceededException::class.java)
    }
}

class FakeMerchantContractService : MerchantContractService {
    private val contracts = mutableMapOf<String, MerchantContract>()

    fun setContract(contract: MerchantContract) {
        contracts[contract.merchantId] = contract
    }

    override fun getContract(merchantId: String): MerchantContract {
        return contracts[merchantId]
            ?: throw IllegalArgumentException("Contract not found for merchant: $merchantId")
    }
}

class FakeMerchantLimitService : MerchantLimitService {
    private val limits = mutableMapOf<String, Long>()

    fun setLimit(merchantId: String, limit: Long) {
        limits[merchantId] = limit
    }

    override fun check(merchantId: String, paymentMethod: PaymentMethodType, amount: Long): Boolean {
        val limit = limits[merchantId] ?: Long.MAX_VALUE
        return amount <= limit
    }

    override fun consume(merchantId: String, paymentId: String, amount: Long) {
        // No-op for fake
    }

    override fun restore(merchantId: String, paymentId: String, amount: Long) {
        // No-op for fake
    }
}
