package com.paybuddy.payment.domain.installment

import com.paybuddy.payment.domain.*

data class IssuerInstallmentPolicy(
    val issuerCode: String,
    val availableMonths: Set<Int>,
    val interestFreeMonths: Set<Int>,
    val minInstallmentAmount: Long = 0
)
