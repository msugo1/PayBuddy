package com.paybuddy.payment.domain

interface PaymentKeyGenerator {
    fun generate(): String
}