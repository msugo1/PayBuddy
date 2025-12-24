package com.paybuddy.payment.domain

import com.paybuddy.payment.domain.PaymentPolicy

/**
 * 결제 금액
 *
 * @property total 총 금액 (원)
 * @property supply 공급가액 (원)
 * @property vat 부가세 (원)
 */
data class PaymentAmount(
    val total: Long,
    val supply: Long,
    val vat: Long
) {
    init {
        require(total >= PaymentPolicy.MIN_PAYMENT_AMOUNT) {
            "Payment amount must be at least ${PaymentPolicy.MIN_PAYMENT_AMOUNT}"
        }
        require(supply >= 0) { "Supply amount cannot be negative" }
        require(vat >= 0) { "VAT amount cannot be negative" }
        require(supply + vat == total) { "Supply + VAT must equal total amount" }
    }
}