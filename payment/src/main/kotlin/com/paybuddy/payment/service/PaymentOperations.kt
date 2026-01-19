package com.paybuddy.payment.service

import com.paybuddy.payment.domain.PaymentMethodType

interface PaymentOperations {
    val paymentMethodType: PaymentMethodType
}
