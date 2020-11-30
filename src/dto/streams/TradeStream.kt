package com.linebizplus.exchange.virtual.dto.streams

import java.math.BigDecimal

/**
 * @param pair 페어
 * @param tradeId 거래ID
 * @param price 체결가격
 * @param amount 체결수량
 * @param buyerOrderId 매수주문ID
 * @param sellerOrderId 매도주문ID
 * @param tradeTime 거래시간
 * @param isTheBuyerTheMarketMaker true: 매수주문이 메이커 false: 매도주문이 메이커
 */
data class TradeStream(
    val pair: String,
    val tradeId: Long,
    val price: BigDecimal,
    val amount: BigDecimal,
    val buyerOrderId: Long,
    val sellerOrderId: Long,
    val tradeTime: Long,
    val isTheBuyerTheMarketMaker: Boolean
)
