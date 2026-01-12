package com.paybuddy.payment.domain.merchant

import com.paybuddy.payment.domain.PaymentMethodType
import com.paybuddy.payment.domain.PaymentPolicy

interface MerchantContractService {
    fun getActiveContract(merchantId: String): MerchantContract
}
