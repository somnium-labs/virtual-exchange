package com.linebizplus.exchange.virtual.services

import com.linebizplus.exchange.virtual.classification.*
import com.linebizplus.exchange.virtual.components.query
import com.linebizplus.exchange.virtual.configuration.AppConfig
import com.linebizplus.exchange.virtual.dto.Order
import com.linebizplus.exchange.virtual.dto.Transaction
import com.linebizplus.exchange.virtual.dto.request.AddOrderRequestDto
import com.linebizplus.exchange.virtual.dto.streams.OrderUpdateStream
import com.linebizplus.exchange.virtual.dto.streams.OrderbookStream
import com.linebizplus.exchange.virtual.dto.streams.TradeStream
import com.linebizplus.exchange.virtual.dto.streams.WebSocketStream
import com.linebizplus.exchange.virtual.entities.BalanceTable
import com.linebizplus.exchange.virtual.entities.OrderTable
import com.linebizplus.exchange.virtual.entities.TransactionTable
import com.linebizplus.exchange.virtual.exceptions.CommonException
import com.linebizplus.exchange.virtual.extensions.*
import com.linebizplus.exchange.virtual.model.Member
import com.linebizplus.exchange.virtual.model.Orderbook
import io.ktor.util.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import java.math.BigDecimal
import java.time.Instant

object OrderService {
    private val orderbooks = mutableMapOf<String, Orderbook>()

    @KtorExperimentalAPI
    suspend fun reset() {
        val pairs = AppConfig.Service.pairs
        for (pair in pairs) {
            orderbooks[pair] = Orderbook(pair)
        }

        // 체결되지 않은 주문이 있다면 모두 취소
        val openOrders = mutableListOf<Order>()

        query {
            openOrders.addAll(
                OrderTable.select {
                    (OrderTable.status.eq(OrderStatus.NEW) or OrderTable.status.eq(OrderStatus.PARTIAL))
                }.map {
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
                })
        }

        openOrders.forEach {
            val member = MemberService.getMemberById(it.memberId)
            if (member != null) {
                cancelOrderQuery(it)
            }
        }
    }

    /**
     * 관리자 계정으로 주문 생성
     * 기존 주문들은 모두 취소한다.
     */
    suspend fun initializeOrdersByAdmin(
        admin: Member,
        pair: String,
        asks: List<List<BigDecimal>>,
        bids: List<List<BigDecimal>>
    ) {
        // 기존 주문들은 모두 취소
        cancelAllOrdersByPair(pair)

        asks.forEach {
            val price = it[0]
            val amount = it[1]
            addOrder(
                admin, AddOrderRequestDto(
                    TimeInForce.GTC,
                    pair, OrderSide.SELL, OrderType.LIMIT, amount, price
                )
            )
        }
        bids.forEach {
            val price = it[0]
            val amount = it[1]
            addOrder(
                admin, AddOrderRequestDto(
                    TimeInForce.GTC,
                    pair, OrderSide.BUY, OrderType.LIMIT, amount, price
                )
            )
        }
    }

    suspend fun cancelAllOrders() {
        orderbooks.forEach {
            cancelAllOrdersByPair(it.key)
        }
    }

    suspend fun cancelAllOrdersByPair(pair: String) {
        orderbooks[pair]?.asks?.forEach { askSpread ->
            askSpread.value.forEach {
                val member = MemberService.getMemberById(it.memberId)
                if (member != null) {
                    cancelOrderQuery(it)
                    member.cancelOrder(it)
                }
            }
        }

        orderbooks[pair]?.bids?.forEach { bidSpread ->
            bidSpread.value.forEach {
                val member = MemberService.getMemberById(it.memberId)
                if (member != null) {
                    cancelOrderQuery(it)
                    member.cancelOrder(it)
                }
            }
        }

        orderbooks[pair]?.asks?.clear()
        orderbooks[pair]?.bids?.clear()
    }

    fun getOrderbookSnapshot(pair: String): OrderbookStream =
        orderbooks[pair]?.createSnapshot() ?: throw IllegalArgumentException("Invalid pair: $pair")

    suspend fun addOrder(member: Member, req: AddOrderRequestDto): Pair<Order, List<Transaction>?> {
        if (req.amount <= BigDecimal.ZERO) {
            throw CommonException(Error.INVALID_ORDER_AMOUNT)
        }

        if (!isEnoughBalance(member, req.pair, req.side, req.type, req.price, req.amount)) {
            throw CommonException(Error.NOT_ENOUGH_BALANCE)
        }

        if (req.type == OrderType.LIMIT && req.price == null) {
            throw CommonException(Error.REQUIRED_PARAMETER)
        }

        if (req.clientOrderId != null && member.findOpenOrderByClientOrderId(req.clientOrderId) != null) {
            throw CommonException(Error.DUPLICATE_CLIENT_ORDER_ID)
        }

        val orderbook = orderbooks[req.pair] ?: throw CommonException(Error.INVALID_PAIR)

        val order = Order(
            req.pair,
            member.id,
            req.clientOrderId,
            generateOrderId(),
            req.price,
            req.amount,
            req.amount,
            OrderStatus.NEW,
            req.type,
            req.side,
            req.timeInForce,
            Instant.now().toEpochMilli()
        )

        query {
            addOrderQuery(order)

            if (order.type == OrderType.LIMIT) {
                when (order.side) {
                    OrderSide.BUY -> {
                        lockBalance(order.memberId, order.pair.quoteAsset(), order.price!! * order.amount)
                    }
                    OrderSide.SELL -> {
                        lockBalance(order.memberId, order.pair.baseAsset(), order.amount)
                    }
                }
            }
        }

        // 주문접수 알림
        member.notify(
            ORDER_UPDATE_CHANNEL, order.pair, OrderUpdateStream(
                order.pair,
                order.orderId,
                order.clientOrderId,
                order.side,
                order.type,
                order.timeInForce,
                order.amount,
                order.price,
                ExecutionType.NEW,
                OrderStatus.NEW
            )
        )

        return when (order.type) {
            OrderType.LIMIT -> {
                when (order.side) {
                    OrderSide.BUY -> {
                        limitBuy(member, order, orderbook)
                    }
                    OrderSide.SELL -> {
                        limitSell(member, order, orderbook)
                    }
                }
            }
            OrderType.MARKET -> {
                when (order.side) {
                    OrderSide.BUY -> marketBuy(member, order, orderbook)
                    OrderSide.SELL -> marketSell(member, order, orderbook)
                }
            }
        }
    }

    suspend fun cancelOrderByOrderId(member: Member, orderId: Long): Order {
        val order = member.findOpenOrderByOrderId(orderId) ?: throw CommonException(Error.NOT_FOUND_ORDER)
        val orderbook = orderbooks[order.pair] ?: throw CommonException(Error.INVALID_PAIR)

        cancelOrderQuery(order)
        orderbook.removeOrder(order)
        member.cancelOrder(order)

        return order
    }

    suspend fun cancelOrderByClientOrderId(member: Member, clientOrderId: String): Order {
        val order =
            member.findOpenOrderByClientOrderId(clientOrderId)
                ?: throw CommonException(Error.NOT_FOUND_ORDER)

        return cancelOrderByOrderId(member, order.orderId)
    }

    private suspend fun limitBuy(
        member: Member,
        order: Order,
        orderbook: Orderbook
    ): Pair<Order, List<Transaction>?> {
        return if (
            order.timeInForce != TimeInForce.IOC
            && (!orderbook.asks.any() || order.price!! < orderbook.asks.firstKey())
        ) {
            orderbook.addOrder(order)
            member.addOrder(order)
            Pair(order, null)
        } else {
            val transactions = tryExecute(member, order, orderbook)
            Pair(order, transactions)
        }
    }

    private suspend fun limitSell(
        member: Member,
        order: Order,
        orderbook: Orderbook
    ): Pair<Order, List<Transaction>?> {
        return if (order.timeInForce != TimeInForce.IOC
            && (!orderbook.bids.any() || order.price!! > orderbook.bids.firstKey())
        ) {
            orderbook.addOrder(order)
            member.addOrder(order)
            Pair(order, null)
        } else {
            val transactions = tryExecute(member, order, orderbook)
            Pair(order, transactions)
        }
    }

    private suspend fun marketBuy(
        member: Member,
        order: Order,
        orderbook: Orderbook
    ): Pair<Order, List<Transaction>?> {
        val transactions = tryExecute(member, order, orderbook)
        return Pair(order, transactions)
    }

    private suspend fun marketSell(
        member: Member,
        order: Order,
        orderbook: Orderbook
    ): Pair<Order, List<Transaction>?> {
        val transactions = tryExecute(member, order, orderbook)
        return Pair(order, transactions)
    }

    /**
     *
     */
    private suspend fun tryExecute(
        taker: Member,
        takerOrder: Order,
        orderbook: Orderbook
    ): List<Transaction> {
        val spreads: MutableMap<BigDecimal, MutableList<Order>> =
            if (takerOrder.side == OrderSide.BUY) orderbook.asks
            else orderbook.bids

        // 오더북에서 삭제할 주문들(주문이 모두 체결되어 수량이 0이 됨)
        val toRemove = mutableListOf<Order>()

        // 테이커 주문의 체결 내역
        val takerTransactions = arrayListOf<Transaction>()

        spreads.forEach SPREAD_LOOP@{ spread ->
            if (takerOrder.remainAmount.isZero()) {
                return@SPREAD_LOOP
            }

            val price = spread.key
            val makerOrders = spread.value.filter { it.remainAmount > BigDecimal.ZERO }

            /*
                시장가 주문: 요청한 수량을 모두 체결할 때까지 순차적으로 호가 탐색
                지정가 매도: 지정가보다 큰 호가까지 순차적으로 탐색
                지정가 매수: 지정가보다 작은 호가까지 순차적으로 탐색
            */
            if (takerOrder.type == OrderType.MARKET
                || (takerOrder.side == OrderSide.SELL && takerOrder.price!! <= price)
                || (takerOrder.side == OrderSide.BUY && takerOrder.price!! >= price)
            ) {
                makerOrders.forEach { makerOrder ->
                    val maker = MemberService.getMemberById(makerOrder.memberId)
                        ?: throw CommonException(Error.NOT_FOUND_MAKER)

                    val amount = makerOrder.remainAmount.min(takerOrder.remainAmount) // 체결수량
                    val tradeId = generateTradeId()
                    val now = Instant.now()

                    val makerTransaction = createTransaction(
                        makerOrder, Liquidity.MAKER, takerOrder.orderId,
                        price, amount, tradeId, now
                    )

                    val takerTransaction = createTransaction(
                        takerOrder, Liquidity.TAKER, makerOrder.orderId,
                        price, amount, tradeId, now
                    )

                    query {
                        execute(now, makerOrder, makerTransaction)
                        execute(now, takerOrder, takerTransaction)
                    }

                    orderbook.matchOrder(makerOrder, takerOrder, amount)

                    // 메이커 체결
                    maker.onTransaction(makerOrder, makerTransaction)

                    // 테이커 체결
                    taker.onTransaction(takerOrder, takerTransaction)

                    // 주문 응답에 제공하기 위한 데이터
                    takerTransactions.add(takerTransaction)

                    if (makerOrder.remainAmount <= BigDecimal.ZERO) {
                        toRemove.add(makerOrder)
                    }
                }
            }
        }

        // 오더북 물량이 부족해서 모든 물량을 체결시키지 못했을 때
        if (BigDecimal.ZERO < takerOrder.remainAmount && takerOrder.timeInForce == TimeInForce.GTC) {
            // 남은 물량을 오더북에 추가
            orderbook.addOrder(takerOrder)
            taker.addOrder(takerOrder)
        } else if (takerOrder.timeInForce == TimeInForce.IOC || takerOrder.type == OrderType.MARKET) {
            // 남은 물량은 취소
            taker.cancelOrder(takerOrder)
        }

        // 모두 체결된 주문은 오더북에서 제거
        toRemove.forEach {
            orderbook.removeOrder(it)
        }

        takerTransactions.forEach {
            val now = Instant.now().toEpochMilli()

            val buyerOrderId =
                if (takerOrder.side == OrderSide.BUY) takerOrder.orderId
                else it.relatedOrderId

            val sellerOrderId =
                if (takerOrder.side == OrderSide.SELL) takerOrder.orderId
                else it.relatedOrderId

            // 테이커의 주문이 매도주문이면, 메이커의 주문은 매수주문이 된다.
            val isTheBuyerTheMarketMaker =
                (it.side == OrderSide.SELL && it.liquidity == Liquidity.TAKER)

            val stream = WebSocketStream(
                TRADE_CHANNEL, it.pair, now, TradeStream(
                    it.pair,
                    it.tradeId,
                    it.price,
                    it.amount,
                    buyerOrderId,
                    sellerOrderId,
                    it.executedTime,
                    isTheBuyerTheMarketMaker
                )
            )
            WebSocketFeedService.broadcast("${it.pair}@$TRADE_CHANNEL", stream)
        }

        return takerTransactions
    }

    private fun createTransaction(
        order: Order,
        liquidity: Liquidity,
        relatedOrderId: Long,
        matchPrice: BigDecimal,
        matchAmount: BigDecimal,
        tradeId: Long,
        now: Instant
    ): Transaction {
        val feeRate =
            if (liquidity == Liquidity.MAKER) MAKER_FEE_RATE
            else TAKER_FEE_RATE

        val fee =
            if (order.side == OrderSide.SELL) feeRate * matchAmount * matchPrice
            else feeRate * matchAmount

        val feeAsset =
            if (order.side == OrderSide.BUY) order.pair.baseAsset()
            else order.pair.quoteAsset()

        return Transaction(
            order.pair,
            order.memberId,
            tradeId,
            order.orderId,
            order.clientOrderId,
            relatedOrderId,
            now.toEpochMilli(),
            matchPrice,
            matchAmount,
            order.side,
            fee,
            feeAsset,
            liquidity
        )
    }

    /**
     * 주문 생성
     */
    private fun addOrderQuery(order: Order) {
        try {
            OrderTable.insert {
                it[pair] = order.pair
                it[memberId] = order.memberId
                it[orderId] = order.orderId
                it[clientOrderId] = order.clientOrderId
                it[price] = order.price
                it[origQty] = order.amount
                it[remainQty] = order.amount
                it[status] = order.status
                it[type] = order.type
                it[side] = order.side
                it[timeInForce] = order.timeInForce
                it[openedTime] = Instant.ofEpochMilli(order.openedTime)
            }
        } catch (e: ExposedSQLException) {
            e.message?.contains("Duplicate")?.let {
                if (it) {
                    throw CommonException(Error.DUPLICATE_CLIENT_ORDER_ID)
                }
            }
            throw e
        }
    }

    /**
     * 거래 체결
     *
     * @param order 원주문
     * @param now 체결시간
     */
    private fun execute(
        now: Instant,
        order: Order,
        transaction: Transaction
    ) {
        insertTradeQuery(now, transaction)
        updateOrderQuery(order, transaction.amount, now)
        updateBalanceQuery(order, transaction.price, transaction.amount, transaction.fee)
    }

    private suspend fun cancelOrderQuery(order: Order) {
        query {
            OrderTable.update({ OrderTable.orderId eq order.orderId }) {
                it[status] = OrderStatus.CANCELED
                it[canceledTime] = Instant.now()
            }

            order.status = OrderStatus.CANCELED
            order.canceledTime = Instant.now().toEpochMilli()

            if (order.type == OrderType.LIMIT) {
                when (order.side) {
                    OrderSide.BUY -> {
                        unlockBalance(
                            order.memberId,
                            order.pair.quoteAsset(),
                            order.remainAmount * order.price!!
                        )
                    }
                    OrderSide.SELL -> {
                        unlockBalance(order.memberId, order.pair.baseAsset(), order.remainAmount)
                    }
                }
            }
        }
    }

    /**
     * 거래정보 추가
     */
    private fun insertTradeQuery(
        now: Instant,
        transaction: Transaction
    ) {
        TransactionTable.insert {
            it[tradeId] = transaction.tradeId
            it[orderId] = transaction.orderId
            it[clientOrderId] = transaction.clientOrderId
            it[relatedOrderId] = transaction.relatedOrderId
            it[memberId] = transaction.memberId
            it[pair] = transaction.pair
            it[side] = transaction.side
            it[price] = transaction.price
            it[amount] = transaction.amount
            it[feeCurrency] = transaction.feeAsset
            it[fee] = transaction.fee
            it[executedTime] = now
            it[liquidity] = transaction.liquidity
        }
    }

    /**
     * 주문상태 업데이트(남은수량, 주문상태)
     * @param order 원주문
     * @param amount 체결수량
     * @param now 체결시간
     */
    private fun updateOrderQuery(order: Order, amount: BigDecimal, now: Instant) {
        OrderTable.update({ OrderTable.orderId eq order.orderId }) {
            it[remainQty] = order.remainAmount - amount
            it[status] =
                if ((order.remainAmount - amount).isZero()) OrderStatus.FILLED
                else OrderStatus.PARTIAL
            it[lastTradeTime] = now
        }
    }

    /**
     * 잔고 업데이트
     *
     * @param order 원주문
     * @param price 체결가격
     * @param amount 체결수량
     */
    private fun updateBalanceQuery(order: Order, price: BigDecimal, amount: BigDecimal, fee: BigDecimal) {
        val baseAsset = order.pair.baseAsset()
        val quoteAsset = order.pair.quoteAsset()

        when (order.side) {
            OrderSide.BUY -> {
                BalanceTable.update({
                    (BalanceTable.memberId eq order.memberId) and (BalanceTable.asset eq baseAsset)
                }) {
                    with(SqlExpressionBuilder) {
                        it.update(BalanceTable.amount, BalanceTable.amount + (amount - fee))
                    }
                }
                BalanceTable.update({
                    (BalanceTable.memberId eq order.memberId) and (BalanceTable.asset eq quoteAsset)
                }) {
                    with(SqlExpressionBuilder) {
                        it.update(BalanceTable.amount, BalanceTable.amount - (price * amount))
                    }
                }
                if (order.type == OrderType.LIMIT) {
                    unlockBalance(order.memberId, quoteAsset, price * amount)
                }
            }
            OrderSide.SELL -> {
                BalanceTable.update({
                    (BalanceTable.memberId eq order.memberId) and (BalanceTable.asset eq baseAsset)
                }) {
                    with(SqlExpressionBuilder) {
                        it.update(BalanceTable.amount, BalanceTable.amount - amount)
                    }
                }
                BalanceTable.update({
                    (BalanceTable.memberId eq order.memberId) and (BalanceTable.asset eq quoteAsset)
                }) {
                    with(SqlExpressionBuilder) {
                        it.update(BalanceTable.amount, BalanceTable.amount + (price * amount - fee))
                    }
                }
                if (order.type == OrderType.LIMIT) {
                    unlockBalance(order.memberId, baseAsset, amount)
                }
            }
        }
    }

    private fun lockBalance(memberId: Long, asset: String, amount: BigDecimal) {
        BalanceTable.update({
            (BalanceTable.memberId eq memberId) and (BalanceTable.asset eq asset)
        }) {
            with(SqlExpressionBuilder) {
                it.update(locked, locked + amount)
            }
        }

        MemberService.getMemberById(memberId)?.lockBalance(asset, amount)
    }

    private fun unlockBalance(memberId: Long, asset: String, amount: BigDecimal) {
        BalanceTable.update({
            (BalanceTable.memberId eq memberId) and (BalanceTable.asset eq asset)
        }) {
            with(SqlExpressionBuilder) {
                it.update(locked, locked - amount)
            }
        }

        MemberService.getMemberById(memberId)?.unlockBalance(asset, amount)
    }

    /**
     * 유저의 잔고가 거래하기 충분한지 체크
     * @param member 사용자
     * @param pair 페어
     * @param side 매수, 매도
     * @param type 지정가, 시장가
     * @param price 요청가격
     * @param amount 요청수량
     */
    private fun isEnoughBalance(
        member: Member,
        pair: String,
        side: OrderSide,
        type: OrderType,
        price: BigDecimal?,
        amount: BigDecimal
    ): Boolean {
        val baseAssetBalance = member.getAvailableBalance(pair.baseAsset())
        val quoteAssetBalance = member.getAvailableBalance(pair.quoteAsset())

        return when (side) {
            OrderSide.SELL -> baseAssetBalance >= amount
            OrderSide.BUY -> {
                return when (type) {
                    OrderType.LIMIT -> {
                        quoteAssetBalance >= price!! * amount
                    }
                    OrderType.MARKET -> {
                        quoteAssetBalance >= getOrderbookSnapshot(pair).asks.first().price * amount
                    }
                }
            }
        }
    }
}
