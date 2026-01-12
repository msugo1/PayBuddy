package com.paybuddy.payment.domain.merchant

import com.paybuddy.payment.domain.PaymentMethodType
import com.paybuddy.payment.domain.PaymentPolicy

enum class MerchantStatus {
    ACTIVE,
    SUSPENDED,
    TERMINATED
}
