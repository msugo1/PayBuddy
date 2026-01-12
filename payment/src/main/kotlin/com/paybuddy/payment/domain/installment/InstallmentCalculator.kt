package com.paybuddy.payment.domain.installment

import com.paybuddy.payment.domain.*
import com.paybuddy.payment.domain.merchant.MerchantInstallmentPolicy
import org.springframework.stereotype.Service

@Service
class InstallmentCalculator {

    fun calculateAvailableOptions(
        merchantPolicy: MerchantInstallmentPolicy,
        issuerPolicy: IssuerInstallmentPolicy,
        cardType: CardType,
        paymentRequestAmount: Long
    ): InstallmentOptions {
        if (cardType != CardType.CREDIT) {
            return InstallmentOptions.UNAVAILABLE
        }

        if (!merchantPolicy.supportsInstallment(paymentRequestAmount)) {
            return InstallmentOptions.UNAVAILABLE
        }

        val availableInstallmentMonths = merchantPolicy.availableMonths
            .intersect(issuerPolicy.availableMonths)

        val availableInterestFreeInstallmentMonths = issuerPolicy.interestFreeMonths
            .intersect(availableInstallmentMonths)

        return InstallmentOptions(
            supported = availableInstallmentMonths.isNotEmpty(),
            availableMonths = availableInstallmentMonths,
            interestFreeMonths = availableInterestFreeInstallmentMonths
        )
    }
}
