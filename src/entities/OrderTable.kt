package com.linebizplus.exchange.mock.entities

import com.linebizplus.exchange.mock.classification.OrderSide
import com.linebizplus.exchange.mock.classification.OrderStatus
import com.linebizplus.exchange.mock.classification.OrderType
import com.linebizplus.exchange.mock.classification.TimeInForce
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.timestamp

object OrderTable : Table("orders") {
    val pair = varchar("pair", 11)
    val memberId = long("member_id")
    val orderId = long("order_id").uniqueIndex()
    val clientOrderId = varchar("client_order_id", 45).nullable()
    val price = decimal("price", 20, 8).nullable()
    val origQty = decimal("amount", 20, 8)
    val remainQty = decimal("remain_amount", 20, 8)
    val status = enumerationByName("status", 8, OrderStatus::class)
    val type = enumerationByName("order_type", 6, OrderType::class)
    val side = enumerationByName("side", 4, OrderSide::class)
    val timeInForce = enumerationByName("time_in_force", 3, TimeInForce::class)
    val openedTime = timestamp("opened_time")
    val canceledTime = timestamp("canceled_time").nullable()
    val lastTradeTime = timestamp("last_trade_time").nullable()

    init {
        index(true, memberId, clientOrderId)
    }
}
