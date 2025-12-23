package com.paybuddy.payment.domain

/**
 * 카드 결제 수단
 *
 * @property maskedNumber 마스킹된 카드 번호
 * @property bin 카드 BIN (Bank Identification Number)
 * @property brand 카드 브랜드
 * @property issuerCode 발급사 코드
 * @property acquirerCode 매입사 코드
 * @property cardType 카드 타입
 * @property ownerType 소유자 타입
 * @property issuedCountry 발급 국가 코드
 * @property productCode 상품 코드
 */
data class Card(
    val maskedNumber: String,
    val bin: String,
    val brand: CardBrand?,
    val issuerCode: String,
    val acquirerCode: String,
    val cardType: CardType,
    val ownerType: OwnerType,
    val issuedCountry: String,
    val productCode: String?
)

/**
 * 카드 브랜드
 */
enum class CardBrand {
    VISA,
    MASTERCARD,
    AMEX,
    JCB,
    UNIONPAY,
    BC,
    LOCAL
}

/**
 * 카드 타입
 */
enum class CardType {
    CREDIT,
    DEBIT,
    PREPAID
}

/**
 * 소유자 타입
 */
enum class OwnerType {
    PERSONAL,
    CORPORATE
}
