package com.paybuddy.payment.domain.installment

import com.paybuddy.payment.domain.*

data class InstallmentOptions(
    val supported: Boolean,
    val availableMonths: Set<Int>,
    val interestFreeMonths: Set<Int>
) {
    fun createInstallment(requestedMonths: Int): Installment {
        if (requestedMonths == 0) {
            return Installment(months = 0, isInterestFree = false)
        }

        require(availableMonths.contains(requestedMonths)) {
            "선택한 할부 개월 수를 사용할 수 없습니다"
        }

        return Installment(
            months = requestedMonths,
            isInterestFree = interestFreeMonths.contains(requestedMonths)
        )
    }

    companion object {
        val UNAVAILABLE = InstallmentOptions(
            supported = false,
            availableMonths = emptySet(),
            interestFreeMonths = emptySet()
        )
    }
}
