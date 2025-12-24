package com.paybuddy.payment.domain

class PaymentSessionConflictException(
    message: String = "동일한 주문에 대해 다른 결제 금액이 요청되었습니다. 주문 정보를 확인해주세요.",
    cause: Throwable? = null
) : RuntimeException(message, cause)