package com.paybuddy.payment.domain

class UnsupportedPaymentMethodException(
    paymentMethodType: PaymentMethodType,
    message: String = "지원하지 않는 결제 수단입니다: ${paymentMethodType.name}",
    cause: Throwable? = null
) : RuntimeException(message, cause)
