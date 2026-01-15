package com.paybuddy.payment.infrastructure

import com.paybuddy.payment.domain.installment.IssuerInstallmentPolicy
import com.paybuddy.payment.domain.installment.IssuerInstallmentPolicyRepository
import org.springframework.stereotype.Component

@Component
class StubIssuerInstallmentPolicyRepository : IssuerInstallmentPolicyRepository {
    override fun findByIssuerCode(issuerCode: String): IssuerInstallmentPolicy {
        return IssuerInstallmentPolicy(
            issuerCode = issuerCode,
            availableMonths = setOf(2, 3, 6, 12),
            interestFreeMonths = setOf(2, 3),
            minInstallmentAmount = 0
        )
    }
}
