package com.paybuddy.payment.domain

/**
 * 주문 상품 항목
 *
 * @property name 상품명
 * @property quantity 수량
 * @property unitAmount 단가 (원)
 * @property imageUrl 상품 이미지 URL
 */
data class OrderLineItem(
    val name: String,
    val quantity: Int,
    val unitAmount: Long,
    val imageUrl: String
) {
    // TODO: 이런 validation이 과연 여기에 있어야 하는걸까?
    //  - API 레이어에서 DTO validation으로 처리해야 하는지?
    //  - Domain Layer는 이미 검증된 데이터만 받아야 하는지?
    //  - Value Object의 불변성 보장을 위해 여기서 검증하는게 맞는지?
    init {
        require(name.isNotBlank()) { "Product name cannot be blank" }
        require(quantity > 0) { "Quantity must be positive" }
        require(unitAmount >= 0) { "Unit amount cannot be negative" }
    }
}
