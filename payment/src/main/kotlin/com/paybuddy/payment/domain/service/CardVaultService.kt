package com.paybuddy.payment.domain.service

import com.paybuddy.payment.domain.*

/**
 * Submit과 Confirm 사이에 민감한 카드 정보를 임시 보관하는 서비스
 *
 * Submit: 카드 검증 후 token 발급 → Confirm: token으로 실제 승인
 */
interface CardVaultService {
    /**
     * 카드 정보 저장 후 토큰 발급
     * @return 토큰 (paymentKey 사용)
     */
    fun store(paymentKey: String, credentials: CardCredentials)

    /**
     * 토큰으로 카드 정보 조회
     * @return 만료되었거나 없으면 null
     */
    fun retrieve(paymentKey: String): CardCredentials?

    /**
     * 카드 정보 삭제 (Confirm 완료 후 즉시 삭제)
     */
    fun delete(paymentKey: String)
}

data class CardCredentials(
    val cardNumber: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvc: String,
    val holderName: String?
)
