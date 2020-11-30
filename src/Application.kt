package com.linebizplus.exchange.virtual

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializationFeature
import com.linebizplus.exchange.virtual.components.DatabaseComponent
import com.linebizplus.exchange.virtual.components.deserialize
import com.linebizplus.exchange.virtual.components.query
import com.linebizplus.exchange.virtual.configuration.AppConfig
import com.linebizplus.exchange.virtual.dto.WebSocketFeed
import com.linebizplus.exchange.virtual.entities.MemberTable
import com.linebizplus.exchange.virtual.routes.adminRouter
import com.linebizplus.exchange.virtual.routes.privateRouter
import com.linebizplus.exchange.virtual.routes.publicRouter
import com.linebizplus.exchange.virtual.services.MemberService
import com.linebizplus.exchange.virtual.services.OrderService
import com.linebizplus.exchange.virtual.services.WebSocketFeedService
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import org.jetbrains.exposed.sql.select
import org.slf4j.event.Level
import java.time.Duration


@KtorExperimentalAPI
suspend fun main(args: Array<String>) {
    val argsMap = args.mapNotNull { it.splitPair('=') }.toMap()
    val profile = argsMap["-profile"]
    AppConfig.loadConfig(profile)

    DatabaseComponent.init()
    MemberService.initialize()
    OrderService.reset()
    return io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

//    install(CORS) {
//        method(HttpMethod.Options)
//        method(HttpMethod.Put)
//        method(HttpMethod.Delete)
//        method(HttpMethod.Patch)
//        header(HttpHeaders.Authorization)
//        header("MyCustomHeader")
//        allowCredentials = true
//        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
//    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
        }
    }

    install(Routing) {
        publicRouter()
        privateRouter()
        adminRouter()
    }

    routing {
        webSocket("/ws/stream") {

            var memberId = 0L
            val socketId = this.toString().substringAfterLast("@")
            WebSocketFeedService.join(socketId, this)

            val apiKey = call.request.queryParameters["apiKey"]
            if (apiKey != null) {
                query {
                    val row = MemberTable.select { MemberTable.apiKey eq apiKey }.singleOrNull()
                    row?.let {
                        memberId = it[MemberTable.id]
                    }
                }
                if (memberId != 0L) {
                    MemberService.addWebSocket(memberId, socketId)
                }
            }

            try {
                while (true) {
                    val text = (incoming.receive() as Frame.Text).readText()
                    val dto = deserialize<WebSocketFeed>(text)
                    when (dto.type) {
                        "subscribe" -> WebSocketFeedService.subscribe(socketId, dto.channels)
                        "unsubscribe" -> WebSocketFeedService.unsubscribe(socketId, dto.channels)
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                println("onClose ${closeReason.await()}")
            } catch (e: Throwable) {
                println("onError ${closeReason.await()}")
            } finally {
                WebSocketFeedService.leave(socketId)
                MemberService.removeWebSocket(memberId, socketId)
            }
        }
    }
}

private fun String.splitPair(ch: Char): Pair<String, String>? = indexOf(ch).let { idx ->
    when (idx) {
        -1 -> null
        else -> Pair(take(idx), drop(idx + 1))
    }
}
