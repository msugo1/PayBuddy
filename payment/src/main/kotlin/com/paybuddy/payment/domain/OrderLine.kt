package com.paybuddy.payment.domain

data class OrderLine(
    val items: List<OrderLineItem>
) {
    init {
        require(items.isNotEmpty()) { "Order must contain at least one item" }
    }
}