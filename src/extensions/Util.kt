package com.linebizplus.exchange.virtual.extensions

import io.ktor.config.*
import io.ktor.util.*
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicLong

val runtimeAt = AtomicLong(System.currentTimeMillis())

/**
 * Returns the sum of all values produced by [selector] function applied to each element in
 * the collection.
 */
inline fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

@KtorExperimentalAPI
fun ApplicationConfigValue.getBoolean() = getString() == "true"

@KtorExperimentalAPI
fun ApplicationConfigValue.getInt() = getString().toInt()

fun String.baseAsset() = split("-")[0]

fun String.quoteAsset() = split("-")[1]

fun generateOrderId() = runtimeAt.addAndGet(1)

fun generateTradeId() = runtimeAt.addAndGet(1)

fun BigDecimal.isEquals(other: BigDecimal) = this.compareTo(other) == 0

fun BigDecimal.isZero() = this.compareTo(BigDecimal.ZERO) == 0
