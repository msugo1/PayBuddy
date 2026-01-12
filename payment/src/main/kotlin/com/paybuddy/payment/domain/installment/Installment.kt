package com.paybuddy.payment.domain.installment

import com.paybuddy.payment.domain.*

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Installment(
    @Column(name = "installment_months")
    val months: Int,

    @Column(name = "installment_interest_free")
    val isInterestFree: Boolean
)
