package com.paybuddy.payment.domain

/**
 * 주문 상품 목록
 *
 * @property items 주문 상품 항목 리스트
 */
data class OrderLine(
    val items: List<OrderLineItem>
) {
    init {
        require(items.isNotEmpty()) { "Order must contain at least one item" }
    }
}