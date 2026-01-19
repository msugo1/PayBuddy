package com.paybuddy.payment.domain.installment

interface IssuerInstallmentPolicyRepository {
    fun findByIssuerCode(issuerCode: String): IssuerInstallmentPolicy
}
