package com.paybuddy.payment.domain

import java.time.LocalDate

data class PaymentMethodPolicy(
    val enabled: Boolean,
    val minAmount: Long? = null,
    val maxAmount: Long? = null
)

data class MerchantInstallmentPolicy(
    val allowedMonths: Set<Int>,
    val interestFreeMonths: Set<Int> = emptySet()
)

data class MerchantContract(
    val merchantId: String,
    val status: MerchantStatus,
    val contractEndDate: LocalDate?,
    val mcc: String,
    val paymentMethodPolicies: Map<PaymentMethodType, PaymentMethodPolicy>,
    val installmentPolicy: MerchantInstallmentPolicy?
)
