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
        finalPaymentAmount: Long
    ): InstallmentOptions {
        if (cardType != CardType.CREDIT) {
            return InstallmentOptions.UNAVAILABLE
        }

        // 가맹점과 발급사 정책 중 더 큰 값 적용 (둘 다 만족해야 함)
        val effectiveMinAmount = maxOf(
            merchantPolicy.minInstallmentAmount,
            issuerPolicy.minInstallmentAmount
        )

        if (!merchantPolicy.supportsInstallment || finalPaymentAmount < effectiveMinAmount) {
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
