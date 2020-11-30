package com.linebizplus.exchange.mock.routes

import com.linebizplus.exchange.mock.exceptions.CommonException
import com.linebizplus.exchange.mock.services.OrderService
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

/**
 * 인증 없이 호출 할 수 있는 endpoint
 */
fun Routing.publicRouter() {
    route("/api/v2") {
        get("/orderbook") {
            try {
                val pair = validateParameter(call.request.queryParameters, "pair")
                call.respond(OrderService.getOrderbookSnapshot(pair))
            } catch (e: CommonException) {
                errorRespond(call, e.error)
            }
        }
    }
}