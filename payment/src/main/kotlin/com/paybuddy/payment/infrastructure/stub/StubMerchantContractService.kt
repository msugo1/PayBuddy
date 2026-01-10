package com.paybuddy.payment.infrastructure.stub

import com.paybuddy.payment.domain.*
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class StubMerchantContractService : MerchantContractService {
    override fun getContract(merchantId: String): MerchantContract {
        return MerchantContract(
            merchantId = merchantId,
            status = MerchantStatus.ACTIVE,
            contractEndDate = LocalDate.now().plusYears(1),
            mcc = "5411",
            paymentMethodPolicies = mapOf(
                PaymentMethodType.CARD to PaymentMethodPolicy(
                    enabled = true,
                    minAmount = null,
                    maxAmount = null
                ),
                PaymentMethodType.VIRTUAL_ACCOUNT to PaymentMethodPolicy(
                    enabled = false
                ),
                PaymentMethodType.SIMPLE_PAYMENT to PaymentMethodPolicy(
                    enabled = false
                )
            ),
            installmentPolicy = MerchantInstallmentPolicy(
                merchantId = merchantId,
                supportsInstallment = true,
                minInstallmentAmount = 50_000,
                availableMonths = setOf(2, 3, 6, 12)
            )
        )
    }
}
