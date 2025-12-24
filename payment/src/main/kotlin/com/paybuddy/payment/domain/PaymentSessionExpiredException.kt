package com.paybuddy.payment.domain

class PaymentSessionExpiredException(
    message: String = "진행중인 결제 세션이 만료되었습니다. 다시 결제를 진행해주세요.",
    cause: Throwable? = null
) : RuntimeException(message, cause)