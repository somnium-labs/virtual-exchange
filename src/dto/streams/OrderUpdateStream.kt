package com.linebizplus.exchange.mock.dto.streams

import com.linebizplus.exchange.mock.classification.*
import java.math.BigDecimal

data class OrderUpdateStream(
    val pair: String,
    val orderId: Long,
    val clientOrderId: String?,
    val side: OrderSide,
    val type: OrderType,
    val timeInForce: TimeInForce,
    val orderAmount: BigDecimal,
    val orderPrice: BigDecimal?,
    val executionType: ExecutionType,
    val orderStatus: OrderStatus,
    val executedAmount: BigDecimal = BigDecimal.ZERO,
    val excutedPrice: BigDecimal = BigDecimal.ZERO,
    val feeAmount: BigDecimal = BigDecimal.ZERO,
    val feeAsset: String? = null,
    val tradeTime: Long? = null,
    val tradeId: Long? = null,
    val liquidity: Liquidity? = null
)
