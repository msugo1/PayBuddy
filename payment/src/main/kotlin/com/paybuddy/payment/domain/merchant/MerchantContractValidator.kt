package com.paybuddy.payment.domain.merchant

import com.paybuddy.payment.domain.PaymentMethodType
import com.paybuddy.payment.domain.PaymentPolicy
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class MerchantContractValidator(
    private val merchantContractService: MerchantContractService,
    private val merchantLimitService: MerchantLimitService,
    private val paymentPolicy: PaymentPolicy
) {
    fun validate(
        merchantId: String,
        paymentMethod: PaymentMethodType,
        amount: Long
    ) {
        val contract = merchantContractService.getActiveContract(merchantId)

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
        /**
         * contractEndDate가 null인 경우 = 무기한 계약으로 간주
         * - 장기 파트너십 가맹점
         * - VIP 계약
         * - 별도 종료 조건이 있는 특별 계약
         * null이면 만료 검증을 스킬하고 통과 처리
         */
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

        /**
         * 플랫폼 전역 정책과 가맹점 정책 중 더 큰 값을 적용
         *
         * 비즈니스 규칙:
         * - 가맹점은 플랫폼 정책보다 엄격하게만 설정 가능 (완화 불가)
         * - 플랫폼 최소금액 = 1,000원, 가맹점 최소금액 = 5,000원 -> 5,000원 적용
         * - 플랫폼 최소금액 = 1,000원, 가맹점 최소금액 = null -> 1,000원 적용
         * - 플랫폼 최소금액 = 1,000원, 가맹점 최소금액 = 500원 -> 1,000원 적용 (가맹점이 임의로 완화 불가)
         *
         * 이유: 플랫폼 정책은 모든 거래에 적용되는 최소 기준이므로 하위 설정으로 우회 불가
         */
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
