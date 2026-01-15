package com.paybuddy.payment.domain

class PaymentSessionNotFoundException(paymentKey: String) :
    IllegalArgumentException("결제 세션을 찾을 수 없습니다: $paymentKey")

class PaymentAlreadySubmittedException(paymentKey: String) :
    IllegalStateException("이미 제출된 결제입니다: $paymentKey")
