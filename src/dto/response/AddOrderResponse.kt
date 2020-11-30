package com.linebizplus.exchange.mock.dto.response

import com.linebizplus.exchange.mock.dto.Order
import com.linebizplus.exchange.mock.dto.Transaction

data class AddOrderResponse(val order: Order, val transactions: List<Transaction>?)
