package com.linebizplus.exchange.virtual.dto.request

import com.linebizplus.exchange.virtual.classification.OrderSide
import com.linebizplus.exchange.virtual.classification.OrderType
import com.linebizplus.exchange.virtual.classification.TimeInForce
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
