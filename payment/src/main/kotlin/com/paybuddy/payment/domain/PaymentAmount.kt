package com.paybuddy.payment.domain

data class PaymentAmount(
    val total: Long,
    val supply: Long,
    val vat: Long
) {
    init {
        require(supply >= 0) { "Supply amount cannot be negative" }
        require(vat >= 0) { "VAT amount cannot be negative" }
        require(supply + vat == total) { "Supply + VAT must equal total amount" }
    }
}