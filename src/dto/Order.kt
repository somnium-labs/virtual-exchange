package com.linebizplus.exchange.mock.dto

import com.linebizplus.exchange.mock.classification.OrderSide
import com.linebizplus.exchange.mock.classification.OrderStatus
import com.linebizplus.exchange.mock.classification.OrderType
import com.linebizplus.exchange.mock.classification.TimeInForce
import java.math.BigDecimal

data class Order(
    val pair: String,
    val memberId: Long,
    val clientOrderId: String?,
    val orderId: Long,
    val price: BigDecimal?,
    val amount: BigDecimal,
    var remainAmount: BigDecimal,
    var status: OrderStatus,
    val type: OrderType,
    val side: OrderSide,
    val timeInForce: TimeInForce = TimeInForce.GTC,
    val openedTime: Long,
    var canceledTime: Long? = null,
    val lastTradeTime: Long? = null
)
