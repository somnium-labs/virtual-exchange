package com.linebizplus.exchange.mock.model

import com.linebizplus.exchange.mock.classification.*
import com.linebizplus.exchange.mock.components.query
import com.linebizplus.exchange.mock.dto.Order
import com.linebizplus.exchange.mock.dto.Transaction
import com.linebizplus.exchange.mock.dto.streams.OrderUpdateStream
import com.linebizplus.exchange.mock.dto.streams.WebSocketStream
import com.linebizplus.exchange.mock.entities.BalanceTable
import com.linebizplus.exchange.mock.entities.OrderTable
import com.linebizplus.exchange.mock.entities.TransactionTable
import com.linebizplus.exchange.mock.exceptions.CommonException
import com.linebizplus.exchange.mock.extensions.baseAsset
import com.linebizplus.exchange.mock.extensions.isZero
import com.linebizplus.exchange.mock.extensions.quoteAsset
import com.linebizplus.exchange.mock.services.WebSocketFeedService
import org.jetbrains.exposed.sql.*
import java.math.BigDecimal
import java.time.Instant

class Member(val id: Long, val name: String, val apiKey: String?) {
    private val socketIds = mutableListOf<String>()
    private val balances = mutableMapOf<String, Balance>()
    private val openOrders = mutableMapOf<Long, Order>()
    private val clientOrderIds = mutableMapOf<String, Long>()

    fun addSocketId(socketId: String) {
        socketIds.add(socketId)
    }

    fun removeSocketId(socketId: String) {
        socketIds.remove(socketId)
    }

    fun initializeBalance(asset: String, available: BigDecimal, holds: BigDecimal) {
        balances[asset] = Balance(asset, available, holds)
    }

    fun getAvailableBalance(asset: String): BigDecimal {
        return balances[asset]?.available ?: BigDecimal.ZERO
    }

    fun getBalance(asset: String?): List<Balance> {
        return if (asset != null) {
            balances.values.filter { it.asset == asset }
        } else {
            balances.values.toList()
        }
    }

    fun lockBalance(asset: String, amount: BigDecimal) {
        balances[asset]?.locked = balances[asset]?.locked?.plus(amount)!!
    }

    fun unlockBalance(asset: String, amount: BigDecimal) {
        balances[asset]?.locked = balances[asset]?.locked?.minus(amount)!!
    }

    suspend fun deposit(asset: String, amount: BigDecimal): Balance {
        if (!balances.containsKey(asset)) {
            throw CommonException(Error.INVALID_ASSET)
        }

        query {
            BalanceTable.update({ BalanceTable.memberId eq id and (BalanceTable.asset eq asset) }) {
                with(SqlExpressionBuilder) {
                    it.update(BalanceTable.amount, BalanceTable.amount + amount)
                }
            }
        }

        balances[asset]?.amount = balances[asset]!!.amount.plus(amount)
        return balances[asset]!!
    }

    suspend fun withdrawal(asset: String, amount: BigDecimal): Balance {
        if (!balances.containsKey(asset)) {
            throw CommonException(Error.INVALID_ASSET)
        }

        val available = balances[asset]?.available ?: BigDecimal.ZERO
        if (available < amount) {
            throw CommonException(Error.NOT_ENOUGH_BALANCE)
        }

        query {
            BalanceTable.update({ BalanceTable.memberId eq id and (BalanceTable.asset eq asset) }) {
                with(SqlExpressionBuilder) {
                    it.update(BalanceTable.amount, BalanceTable.amount - amount)
                }
            }
        }

        balances[asset]?.amount = balances[asset]!!.amount.minus(amount)
        return balances[asset]!!
    }

    fun addOrder(order: Order) {
        openOrders.putIfAbsent(order.orderId, order)
        if (order.clientOrderId != null) {
            clientOrderIds[order.clientOrderId] = order.orderId
        }
    }

    suspend fun cancelOrder(order: Order) {
        openOrders.remove(order.orderId)
        clientOrderIds.remove(order.clientOrderId)

        notify(
            ORDER_UPDATE_CHANNEL, order.pair, OrderUpdateStream(
                order.pair,
                order.orderId,
                order.clientOrderId,
                order.side,
                order.type,
                order.timeInForce,
                order.amount,
                order.price,
                ExecutionType.CANCELED,
                order.status
            )
        )
    }

    fun getOpenOrders(pair: String?): List<Order> {
        return if (pair != null) {
            openOrders.values.filter { it.pair == pair }
        } else {
            openOrders.values.toList()
        }
    }

    suspend fun getTrades(
        pair: String,
        fromId: Long?,
        start: Instant?,
        end: Instant?,
        limit: Int,
    ): List<Transaction> {
        return query {
            val condition = when {
                fromId != null && start != null && end != null -> {
                    Op.build {
                        (TransactionTable.memberId eq id) and
                                (TransactionTable.pair eq pair) and
                                (TransactionTable.tradeId greaterEq fromId) and
                                (TransactionTable.executedTime greaterEq start) and
                                (TransactionTable.executedTime lessEq end)
                    }
                }
                fromId != null && start != null -> {
                    Op.build {
                        (TransactionTable.memberId eq id) and
                                (TransactionTable.pair eq pair) and
                                (TransactionTable.tradeId greaterEq fromId) and
                                (TransactionTable.executedTime greaterEq start)
                    }
                }
                fromId != null && end != null -> {
                    Op.build {
                        (TransactionTable.memberId eq id) and
                                (TransactionTable.pair eq pair) and
                                (TransactionTable.tradeId greaterEq fromId) and
                                (TransactionTable.executedTime lessEq end)
                    }
                }
                fromId != null -> {
                    Op.build {
                        (TransactionTable.memberId eq id) and
                                (TransactionTable.pair eq pair) and
                                (TransactionTable.tradeId greaterEq fromId)
                    }
                }
                start != null -> {
                    Op.build {
                        (TransactionTable.memberId eq id) and
                                (TransactionTable.pair eq pair) and
                                (TransactionTable.executedTime greaterEq start)
                    }
                }
                end != null -> {
                    Op.build {
                        (TransactionTable.memberId eq id) and
                                (TransactionTable.pair eq pair) and
                                (TransactionTable.executedTime lessEq end)
                    }
                }
                else -> Op.build { TransactionTable.memberId eq id and (TransactionTable.pair eq pair) }
            }

            return@query TransactionTable.select(condition).limit(limit)
                .orderBy(TransactionTable.tradeId to SortOrder.DESC).map {
                    Transaction(
                        it[TransactionTable.pair],
                        it[TransactionTable.memberId],
                        it[TransactionTable.tradeId],
                        it[TransactionTable.orderId],
                        it[TransactionTable.clientOrderId],
                        it[TransactionTable.relatedOrderId],
                        it[TransactionTable.executedTime].toEpochMilli(),
                        it[TransactionTable.price],
                        it[TransactionTable.amount],
                        it[TransactionTable.side],
                        it[TransactionTable.fee],
                        it[TransactionTable.feeCurrency],
                        it[TransactionTable.liquidity]
                    )
                }
        }
    }


    suspend fun findOrderByOrderId(orderId: Long): Order? {
        return query {
            return@query OrderTable.select { OrderTable.orderId eq orderId }.map {
                Order(
                    it[OrderTable.pair],
                    it[OrderTable.memberId],
                    it[OrderTable.clientOrderId],
                    it[OrderTable.orderId],
                    it[OrderTable.price],
                    it[OrderTable.origQty],
                    it[OrderTable.remainQty],
                    it[OrderTable.status],
                    it[OrderTable.type],
                    it[OrderTable.side],
                    it[OrderTable.timeInForce],
                    it[OrderTable.openedTime].toEpochMilli(),
                    it[OrderTable.lastTradeTime]?.toEpochMilli()
                )
            }.singleOrNull()
        }
    }

    suspend fun findOrderByOrderId(clientOrderId: String): Order? {
        val orderId = clientOrderIds[clientOrderId]
        return if (orderId == null) {
            null
        } else {
            findOrderByOrderId(orderId)
        }
    }

    fun findOpenOrderByOrderId(orderId: Long): Order? {
        return openOrders[orderId]
    }

    fun findOpenOrderByClientOrderId(clientOrderId: String): Order? {
        val orderId = clientOrderIds[clientOrderId]
        return if (orderId == null) {
            null
        } else {
            findOpenOrderByOrderId(orderId)
        }
    }

    suspend fun onTransaction(order: Order, transaction: Transaction) {
        val feeRate =
            if (transaction.liquidity == Liquidity.MAKER) MAKER_FEE_RATE
            else TAKER_FEE_RATE

        val fee =
            if (transaction.side == OrderSide.SELL) feeRate * transaction.amount * transaction.price
            else feeRate * transaction.amount

        val baseAsset = transaction.pair.baseAsset()
        val quoteAsset = transaction.pair.quoteAsset()

        when (transaction.side) {
            OrderSide.BUY -> {
                balances[baseAsset]?.amount =
                    balances[baseAsset]?.amount?.plus(transaction.amount - fee)!!

                if (transaction.liquidity == Liquidity.MAKER) {
                    balances[quoteAsset]?.locked =
                        balances[quoteAsset]?.locked?.minus((transaction.price * transaction.amount))!!
                }
            }
            OrderSide.SELL -> {
                balances[quoteAsset]?.amount =
                    balances[quoteAsset]?.amount?.plus((transaction.price * transaction.amount - fee))!!

                if (transaction.liquidity == Liquidity.MAKER) {
                    balances[baseAsset]?.locked =
                        balances[baseAsset]?.locked?.minus((transaction.amount))!!
                }
            }
        }

        order.status =
            if (order.remainAmount.isZero()) OrderStatus.FILLED
            else OrderStatus.PARTIAL

        if (order.status == OrderStatus.FILLED) {
            openOrders.remove(order.orderId)
        }

        val tradeStream = OrderUpdateStream(
            order.pair,
            order.orderId,
            order.clientOrderId,
            order.side,
            order.type,
            order.timeInForce,
            order.amount,
            order.price,
            ExecutionType.TRADE,
            order.status,
            transaction.amount,
            transaction.price,
            transaction.fee,
            transaction.feeAsset,
            transaction.executedTime,
            transaction.tradeId,
            transaction.liquidity
        )

        notify(ORDER_UPDATE_CHANNEL, transaction.pair, tradeStream)
    }

    suspend fun <T> notify(channel: String, pair: String, data: T) {
        val now = Instant.now().toEpochMilli()
        val stream = WebSocketStream(channel, pair, now, data)

        socketIds.forEach {
            WebSocketFeedService.send(it, stream)
        }
    }
}
