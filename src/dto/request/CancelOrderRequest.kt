package com.linebizplus.exchange.virtual.dto.request

data class CancelOrderRequest(val pair: String, val orderId: Long?, val clientOrderId: String?)
