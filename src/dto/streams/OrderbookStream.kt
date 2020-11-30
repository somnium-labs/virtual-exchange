package com.linebizplus.exchange.virtual.dto.streams

import com.linebizplus.exchange.virtual.dto.Spread


data class OrderbookStream(val lastUpdatedId: Long, val asks: List<Spread>, val bids: List<Spread>)

