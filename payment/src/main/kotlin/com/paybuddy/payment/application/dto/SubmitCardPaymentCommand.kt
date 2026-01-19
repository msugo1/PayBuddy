package com.paybuddy.payment.application.dto

data class SubmitCardPaymentCommand(
    val paymentKey: String,
    val cardNumber: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvc: String,
    val holderName: String?,
    val installmentMonths: Int
)

// 다른 결제수단을 어떻게 받아올 것인지