package com.linebizplus.exchange.mock.dto

import com.linebizplus.exchange.mock.classification.Liquidity
import com.linebizplus.exchange.mock.classification.OrderSide
import java.math.BigDecimal

data class Transaction(
    val pair: String,
    val memberId: Long,
    val tradeId: Long,
    val orderId: Long,
    val clientOrderId: String? = null,
    val relatedOrderId: Long,
    val executedTime: Long,
    val price: BigDecimal,
    val amount: BigDecimal,
    val side: OrderSide,
    val fee: BigDecimal,
    val feeAsset: String,
    val liquidity: Liquidity
)
