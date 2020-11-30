package com.linebizplus.exchange.mock.dto.request

import java.math.BigDecimal

data class InitializeOrderbookRequest(
    val pair: String,
    val asks: List<List<BigDecimal>>,
    val bids: List<List<BigDecimal>>
)
