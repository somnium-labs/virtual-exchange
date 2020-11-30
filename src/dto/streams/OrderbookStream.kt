package com.linebizplus.exchange.mock.dto.streams

import com.linebizplus.exchange.mock.dto.Spread


data class OrderbookStream(val lastUpdatedId: Long, val asks: List<Spread>, val bids: List<Spread>)

