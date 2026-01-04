package com.paybuddy.payment.domain

/** 동일한 주문에 대한 중복 결제 요청 */
class DuplicatePaymentRequestException(
    val merchantId: String,
    val orderId: String
) : RuntimeException("Duplicate payment request for merchantId=$merchantId, orderId=$orderId")
