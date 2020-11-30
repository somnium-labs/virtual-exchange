package com.linebizplus.exchange.virtual.dto.response

import com.linebizplus.exchange.virtual.dto.Order
import com.linebizplus.exchange.virtual.dto.Transaction

data class AddOrderResponse(val order: Order, val transactions: List<Transaction>?)
