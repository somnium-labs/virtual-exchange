package com.linebizplus.exchange.mock.services

import com.linebizplus.exchange.mock.model.Subscriber
import io.ktor.http.cio.websocket.*
import java.util.concurrent.ConcurrentHashMap

object WebSocketFeedService {
    private val subscribers = ConcurrentHashMap<String, Subscriber>()

    fun join(socketId: String, socket: WebSocketSession) {
        subscribers[socketId] = Subscriber(socket)
    }

    fun leave(socketId: String) {
        subscribers.remove(socketId)
    }

    fun subscribe(socketId: String, channels: List<String>) {
        channels.forEach {
            subscribers[socketId]?.subscribe(it)
        }
    }

    fun unsubscribe(socketId: String, channels: List<String>) {
        channels.forEach {
            subscribers[socketId]?.unsubscribe(it)
        }
    }

    suspend fun <T> send(socketId: String, value: T) {
        subscribers[socketId]?.send(value)
    }

    suspend fun <T> broadcast(channel: String, value: T) {
        subscribers.values.forEach {
            if (it.channels.contains(channel)) {
                it.send(value)
            }
        }
    }
}
