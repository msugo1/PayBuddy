package com.paybuddy.payment.domain

data class InstallmentOptions(
    val supported: Boolean,
    val availableMonths: Set<Int>,
    val interestFreeMonths: Set<Int>
) {
    companion object {
        val UNAVAILABLE = InstallmentOptions(
            supported = false,
            availableMonths = emptySet(),
            interestFreeMonths = emptySet()
        )
    }
}
