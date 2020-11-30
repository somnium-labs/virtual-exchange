package com.linebizplus.exchange.virtual.entities

import com.linebizplus.exchange.virtual.classification.Liquidity
import com.linebizplus.exchange.virtual.classification.OrderSide
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.timestamp

object TransactionTable : Table("transactions") {
    val tradeId = long("trade_id")
    val orderId = (long("order_id") references OrderTable.orderId)
    val clientOrderId = varchar("client_order_id", 45).nullable()
    val relatedOrderId = long("related_order_id")
    val memberId = (long("member_id") references MemberTable.id)
    val pair = varchar("pair", 45)
    val side = enumerationByName("side", 4, OrderSide::class)
    val price = decimal("price", 20, 8)
    val amount = decimal("amount", 20, 8)
    val feeCurrency = varchar("fee_currency", 5)
    val fee = decimal("fee", 20, 8)
    val executedTime = timestamp("executed_time")
    val liquidity = enumerationByName("liquidity", 5, Liquidity::class)

    override val primaryKey = PrimaryKey(tradeId, orderId)
}
