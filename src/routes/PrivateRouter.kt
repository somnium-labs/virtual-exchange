package com.linebizplus.exchange.virtual.routes

import com.linebizplus.exchange.virtual.classification.*
import com.linebizplus.exchange.virtual.dto.request.AddOrderRequestDto
import com.linebizplus.exchange.virtual.dto.response.AddOrderResponse
import com.linebizplus.exchange.virtual.exceptions.CommonException
import com.linebizplus.exchange.virtual.services.OrderService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import java.math.BigDecimal
import java.time.Instant
import kotlin.math.min

/**
 * 사용자 인증이 필요한 endpoint
 * 주문 및 계정 관리에 사용
 * 모든 요청은 등록된 API Key를 통해서만 실행 가능
 *
 * Content-Type: application/json
 */

fun Routing.privateRouter() {
    route("/api/v2") {
        post("/order") {
            try {
                val member = validateMember(call.request.header(HEADER_API_KEY))
                val parameters = call.receiveParameters()
                val dto = validateOrder(parameters)
                val result = OrderService.addOrder(member, dto)
                val order = result.first
                val transactions = result.second
                call.respond(AddOrderResponse(order, transactions))
            } catch (e: CommonException) {
                errorRespond(call, e.error)
            }
        }

        delete("/order") {
            try {
                val member = validateMember(call.request.header(HEADER_API_KEY))
                val orderId = call.request.queryParameters["orderId"]
                if (orderId != null) {
                    call.respond(OrderService.cancelOrderByOrderId(member, orderId.toLong()))
                } else {
                    val clientOrderId = validateParameter(call.request.queryParameters, "clientOrderId")
                    call.respond(OrderService.cancelOrderByClientOrderId(member, clientOrderId))
                }
            } catch (e: CommonException) {
                errorRespond(call, e.error)
            }
        }

        get("/queryOrder") {
            try {
                val member = validateMember(call.request.header(HEADER_API_KEY))
                val orderId = call.request.queryParameters["orderId"]
                if (orderId != null) {
                    call.respond(
                        member.findOrderByOrderId(orderId.toLong())
                            ?: throw CommonException(Error.NOT_FOUND_ORDER)
                    )
                } else {
                    val clientOrderId = validateParameter(call.request.queryParameters, "clientOrderId")
                    call.respond(
                        member.findOrderByOrderId(clientOrderId)
                            ?: throw CommonException(Error.NOT_FOUND_ORDER)
                    )
                }
            } catch (e: CommonException) {
                errorRespond(call, e.error)
            }
        }

        get("/openOrders") {
            val member = validateMember(call.request.header(HEADER_API_KEY))
            val pair = call.request.queryParameters["pair"]
            call.respond(member.getOpenOrders(pair))
        }

        get("/trades") {
            val member = validateMember(call.request.header(HEADER_API_KEY))
            val pair = validateParameter(call.request.queryParameters, "pair")
            val fromId = call.request.queryParameters["fromId"]
            val startTime = call.request.queryParameters["startTime"]
            val endTime = call.request.queryParameters["endTime"]
            val limit = call.request.queryParameters["limit"]

            call.respond(
                member.getTrades(
                    pair,
                    fromId?.toLong(),
                    startTime?.toLong()?.let { Instant.ofEpochMilli(it) },
                    endTime?.toLong()?.let { Instant.ofEpochMilli(it) },
                    limit?.toInt()?.let { min(it, 1000) } ?: 500
                )
            )
        }

        get("/balances") {
            val member = validateMember(call.request.header(HEADER_API_KEY))
            val asset = call.request.queryParameters["asset"]
            call.respond(member.getBalance(asset))
        }

        post("/deposit") {
            try {
                val member = validateMember(call.request.header(HEADER_API_KEY))
                val parameters = call.receiveParameters()
                val asset = validateParameter(parameters, "asset")
                val amount = validateParameter(parameters, "amount")
                call.respond(member.deposit(asset, BigDecimal(amount)))
            } catch (e: CommonException) {
                errorRespond(call, e.error)
            }
        }

        post("/withdrawal") {
            try {
                val member = validateMember(call.request.header(HEADER_API_KEY))
                val parameters = call.receiveParameters()
                val asset = validateParameter(parameters, "asset")
                val amount = validateParameter(parameters, "amount")
                call.respond(member.withdrawal(asset, BigDecimal(amount)))
            } catch (e: CommonException) {
                errorRespond(call, e.error)
            }
        }
    }
}


private fun validateOrder(queryParameters: Parameters): AddOrderRequestDto {
    val pair = validateParameter(queryParameters, "pair")
    val side = validateParameter(queryParameters, "side")
    val type = validateParameter(queryParameters, "type")
    val amount = validateParameter(queryParameters, "amount")
    val price = validateParameter(queryParameters, "price")

    val timeInForce = queryParameters["timeInForce"] ?: "GTC"
    val clientOrderId = queryParameters["clientOrderId"]

    return AddOrderRequestDto(
        TimeInForce.valueOf(timeInForce.toUpperCase()),
        pair,
        OrderSide.valueOf(side.toUpperCase()),
        OrderType.valueOf(type.toUpperCase()),
        BigDecimal(amount),
        BigDecimal(price),
        clientOrderId
    )
}


