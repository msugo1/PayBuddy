package com.paybuddy.payment.config

import com.paybuddy.payment.domain.service.CardCredentials
import com.paybuddy.payment.domain.service.CardVaultService

/**
 * Contract Test용 Stub 구현체
 * 메모리에 카드 정보를 임시 저장합니다.
 */
class StubCardVaultService : CardVaultService {

    private val storage = mutableMapOf<String, CardCredentials>()

    override fun store(paymentKey: String, credentials: CardCredentials) {
        storage[paymentKey] = credentials
    }

    override fun retrieve(paymentKey: String): CardCredentials? {
        return storage[paymentKey]
    }

    override fun delete(paymentKey: String) {
        storage.remove(paymentKey)
    }
}
