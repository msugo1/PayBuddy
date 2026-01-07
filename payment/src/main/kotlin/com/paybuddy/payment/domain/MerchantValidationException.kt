package com.paybuddy.payment.domain

sealed class MerchantValidationException(message: String) : RuntimeException(message)

class ContractExpiredException(merchantId: String) :
    MerchantValidationException("가맹점 계약이 만료되었습니다: $merchantId")

class MerchantSuspendedException(merchantId: String) :
    MerchantValidationException("가맹점이 정지 상태입니다: $merchantId")

class MerchantTerminatedException(merchantId: String) :
    MerchantValidationException("가맹점 계약이 해지되었습니다: $merchantId")

class PaymentMethodNotAllowedException(merchantId: String, paymentMethod: PaymentMethodType) :
    MerchantValidationException("해당 결제수단이 허용되지 않습니다: merchantId=$merchantId, paymentMethod=$paymentMethod")

class AmountBelowMinimumException(amount: Long, minAmount: Long) :
    MerchantValidationException("결제 금액이 최소 금액보다 작습니다: amount=$amount, minAmount=$minAmount")

class MerchantLimitExceededException(merchantId: String) :
    MerchantValidationException("가맹점 한도를 초과했습니다: $merchantId")
