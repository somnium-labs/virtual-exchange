package com.linebizplus.exchange.virtual.components

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

object JsonComponent {
    val mapper = jacksonObjectMapper()

    init {
        mapper.enable(SerializationFeature.INDENT_OUTPUT)
    }
}

fun <T> serialize(value: T): String {
    return JsonComponent.mapper.writeValueAsString(value)
}

inline fun <reified T> deserialize(json: String): T {
    return JsonComponent.mapper.readValue(json)
}
