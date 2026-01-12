package com.paybuddy.payment.domain.merchant

import com.paybuddy.payment.domain.PaymentMethodType
import com.paybuddy.payment.domain.PaymentPolicy

import java.time.LocalDate

data class PaymentMethodPolicy(
    val enabled: Boolean,
    val minAmount: Long? = null,
    val maxAmount: Long? = null
)

data class MerchantInstallmentPolicy(
    val merchantId: String,
    val supportsInstallment: Boolean,
    val minInstallmentAmount: Long,
    val availableMonths: Set<Int>
) {
    fun supportsInstallment(amount: Long): Boolean {
        return supportsInstallment && amount >= minInstallmentAmount
    }
}

data class MerchantContract(
    val merchantId: String,
    val status: MerchantStatus,
    val contractEndDate: LocalDate?,
    val mcc: String,
    val paymentMethodPolicies: Map<PaymentMethodType, PaymentMethodPolicy>,
    val installmentPolicy: MerchantInstallmentPolicy
)
