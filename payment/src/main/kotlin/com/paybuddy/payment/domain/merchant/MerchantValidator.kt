package com.paybuddy.payment.domain.merchant

import com.paybuddy.payment.domain.PaymentMethodType
import com.paybuddy.payment.domain.PaymentPolicy
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class MerchantValidator(
    private val merchantContractService: MerchantContractService,
    private val merchantLimitService: MerchantLimitService,
    private val paymentPolicy: PaymentPolicy
) {
    fun validate(
        merchantId: String,
        paymentMethod: PaymentMethodType,
        amount: Long
    ) {
        val contract = merchantContractService.getContract(merchantId)

        validateMerchantStatus(contract)
        validateContractNotExpired(contract)
        validatePaymentMethodAllowed(contract, paymentMethod)
        validateMinimumAmount(contract, paymentMethod, amount)
        validateMerchantLimit(merchantId, paymentMethod, amount)
    }

    private fun validateMerchantStatus(contract: MerchantContract) {
        when (contract.status) {
            MerchantStatus.ACTIVE -> return
            MerchantStatus.SUSPENDED -> throw MerchantSuspendedException(contract.merchantId)
            MerchantStatus.TERMINATED -> throw MerchantTerminatedException(contract.merchantId)
        }
    }

    private fun validateContractNotExpired(contract: MerchantContract) {
        val endDate = contract.contractEndDate ?: return
        if (endDate.isBefore(LocalDate.now())) {
            throw ContractExpiredException(contract.merchantId)
        }
    }

    private fun validatePaymentMethodAllowed(
        contract: MerchantContract,
        paymentMethod: PaymentMethodType
    ) {
        val policy = contract.paymentMethodPolicies[paymentMethod]
            ?: throw PaymentMethodNotAllowedException(contract.merchantId, paymentMethod)

        if (!policy.enabled) {
            throw PaymentMethodNotAllowedException(contract.merchantId, paymentMethod)
        }
    }

    private fun validateMinimumAmount(
        contract: MerchantContract,
        paymentMethod: PaymentMethodType,
        amount: Long
    ) {
        val globalMinAmount = paymentPolicy.minPaymentAmount
        val merchantMinAmount = contract.paymentMethodPolicies[paymentMethod]?.minAmount

        val effectiveMinAmount = maxOf(globalMinAmount, merchantMinAmount ?: 0L)

        if (amount < effectiveMinAmount) {
            throw AmountBelowMinimumException(amount, effectiveMinAmount)
        }
    }

    private fun validateMerchantLimit(
        merchantId: String,
        paymentMethod: PaymentMethodType,
        amount: Long
    ) {
        val withinLimit = merchantLimitService.check(merchantId, paymentMethod, amount)
        if (!withinLimit) {
            throw MerchantLimitExceededException(merchantId)
        }
    }
}
