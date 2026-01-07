package com.paybuddy.payment.domain

interface MerchantContractService {
    fun getContract(merchantId: String): MerchantContract
}
