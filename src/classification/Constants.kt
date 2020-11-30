package com.linebizplus.exchange.mock.classification

import java.math.BigDecimal

const val HEADER_API_KEY = "X-API-KEY"

// WebSocket Event Name
const val TRADE_CHANNEL = "Trades"
const val ORDERBOOK_CHANNEL = "OrderBook"
const val ORDER_UPDATE_CHANNEL = "OrderUpdate"

val MAKER_FEE_RATE = BigDecimal(0.0)
val TAKER_FEE_RATE = BigDecimal(0.0)
