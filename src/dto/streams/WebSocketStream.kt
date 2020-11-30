package com.linebizplus.exchange.virtual.dto.streams

data class WebSocketStream<T>(val channel: String, val pair: String, val eventTime: Long, val data: T)
