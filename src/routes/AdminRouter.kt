package com.linebizplus.exchange.mock.routes

import com.linebizplus.exchange.mock.classification.HEADER_API_KEY
import com.linebizplus.exchange.mock.dto.request.InitializeOrderbookRequest
import com.linebizplus.exchange.mock.exceptions.CommonException
import com.linebizplus.exchange.mock.services.OrderService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Routing.adminRouter() {
    route("/api/admin/v2") {
        post("/orderbook") {
            val admin = validateMember(call.request.header(HEADER_API_KEY))
            val dto = call.receive<InitializeOrderbookRequest>()
            OrderService.initializeOrdersByAdmin(admin, dto.pair, dto.asks, dto.bids)
            call.respond(HttpStatusCode.OK)
        }
        delete("/orderbook") {
            try {
                validateMember(call.request.header(HEADER_API_KEY))
                val pair = call.request.queryParameters["pair"]
                if (pair != null) {
                    OrderService.cancelAllOrdersByPair(pair)
                } else {
                    OrderService.cancelAllOrders()
                }
                call.respond(HttpStatusCode.OK)
            } catch (e: CommonException) {
                errorRespond(call, e.error)
            }
        }
    }
}
