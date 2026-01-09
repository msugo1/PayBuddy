package com.paybuddy.payment.domain

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(id: String): Payment?
    fun findByPaymentKey(paymentKey: String): Payment?
}
