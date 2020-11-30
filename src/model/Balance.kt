package com.linebizplus.exchange.virtual.model

import java.math.BigDecimal

data class Balance(val asset: String, var amount: BigDecimal, var locked: BigDecimal) {
    val available: BigDecimal get() = amount - locked
}
