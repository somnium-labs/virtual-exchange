package com.linebizplus.exchange.virtual.model


import com.linebizplus.exchange.virtual.classification.ORDERBOOK_CHANNEL
import com.linebizplus.exchange.virtual.classification.OrderSide
import com.linebizplus.exchange.virtual.dto.Order
import com.linebizplus.exchange.virtual.dto.Spread
import com.linebizplus.exchange.virtual.dto.streams.OrderbookStream
import com.linebizplus.exchange.virtual.dto.streams.WebSocketStream
import com.linebizplus.exchange.virtual.extensions.sumByBigDecimal
import com.linebizplus.exchange.virtual.services.WebSocketFeedService
import java.math.BigDecimal
import java.time.Instant

class Orderbook(private val pair: String) {
    val asks = sortedMapOf<BigDecimal, MutableList<Order>>()
    val bids = sortedMapOf<BigDecimal, MutableList<Order>>(reverseOrder())

    fun createSnapshot(): OrderbookStream {
        val lastUpdatedId = Instant.now().toEpochMilli()
        val asks = asks.map { ask -> Spread(ask.key, ask.value.sumByBigDecimal { it.remainAmount }) }
        val bids = bids.map { bid -> Spread(bid.key, bid.value.sumByBigDecimal { it.remainAmount }) }
        return OrderbookStream(lastUpdatedId, asks.sortedByDescending { it.price }, bids)
    }

    /**
     *  Add the order to the order book.
     *
     *  @param order Verified order
     */
    suspend fun addOrder(order: Order) {
        if (order.price != null) {
            when (order.side) {
                OrderSide.BUY -> bids[order.price]?.add(order) ?: bids.put(order.price, mutableListOf(order))
                OrderSide.SELL -> asks[order.price]?.add(order) ?: asks.put(order.price, mutableListOf(order))
            }

            broadcastOrderbook()
        }
    }

    suspend fun matchOrder(makerOrder: Order, takerOrder: Order, matchAmount: BigDecimal) {
        takerOrder.remainAmount -= matchAmount
        makerOrder.remainAmount -= matchAmount

        broadcastOrderbook()
    }

    suspend fun removeOrder(order: Order) {
        val spreads =
            when (order.side) {
                OrderSide.SELL -> asks
                OrderSide.BUY -> bids
            }

        // 호가내에서 삭제
        spreads[order.price]?.remove(order)

        // 오더북에서 해당 호가 삭제
        if (!spreads[order.price]?.any()!!) {
            spreads.remove(order.price)
        }

        broadcastOrderbook()
    }

    private suspend fun broadcastOrderbook() {
        val now = Instant.now().toEpochMilli()
        val stream = WebSocketStream(ORDERBOOK_CHANNEL, pair, now, createSnapshot())
        WebSocketFeedService.broadcast("$pair@$ORDERBOOK_CHANNEL", stream)
    }
}

