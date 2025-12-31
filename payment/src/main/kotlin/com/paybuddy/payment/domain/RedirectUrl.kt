package com.paybuddy.payment.domain

import jakarta.persistence.Embeddable

@Embeddable
data class RedirectUrl(
    val success: String,
    val fail: String
)