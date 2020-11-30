package com.linebizplus.exchange.mock.dto.request

data class CancelOrderRequest(val pair: String, val orderId: Long?, val clientOrderId: String?)
