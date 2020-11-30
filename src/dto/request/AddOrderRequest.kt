package com.linebizplus.exchange.mock.dto.request

import com.linebizplus.exchange.mock.classification.OrderSide
import com.linebizplus.exchange.mock.classification.OrderType
import com.linebizplus.exchange.mock.classification.TimeInForce
import java.math.BigDecimal

data class AddOrderRequestDto(
    val timeInForce: TimeInForce,
    val pair: String,
    val side: OrderSide,
    val type: OrderType,
    val amount: BigDecimal,
    val price: BigDecimal?,
    val clientOrderId: String? = null
)
