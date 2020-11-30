package com.linebizplus.exchange.mock.model

import com.linebizplus.exchange.mock.components.serialize
import io.ktor.http.cio.websocket.*
import java.util.concurrent.CancellationException

class Subscriber(private val session: WebSocketSession) {
    val channels = mutableListOf<String>()

    fun subscribe(channel: String) {
        channels.add(channel)
    }

    fun unsubscribe(channel: String) {
        channels.remove(channel)
    }

    suspend fun <T> send(value: T) {
        try {
            session.send(Frame.Text(serialize(value)))
        } catch (e: CancellationException) {
            println(e.message)
        }
    }
}
