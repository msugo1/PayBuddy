package com.paybuddy.payment

import com.paybuddy.payment.domain.OrderLine
import com.paybuddy.payment.domain.OrderLineItem

val DEFAULT_ORDER_LINE = OrderLine(
    items = listOf(
        OrderLineItem(
            name = "테스트상품",
            quantity = 1,
            unitAmount = 10000,
            imageUrl = "https://example.com/image.jpg"
        )
    )
)
