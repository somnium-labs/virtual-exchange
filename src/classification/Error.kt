package com.linebizplus.exchange.mock.classification

enum class Error(val message: String, val code: Int) {
    INVALID_API_KEY("Invalid API Key", -1001),
    PERMISSION_DENIED("Permission denied", -1002),
    REQUIRED_PARAMETER("Required parameter", -1005),
    INVALID_PAIR("Invalid pair", -1121),
    INVALID_ASSET("Invalid asset", -1122),
    NOT_FOUND_ORDER("Not found order", -2013),
    NOT_FOUND_MAKER("Not found maker", -2014),
    INVALID_ORDER_AMOUNT("Order amount cannot be less than zero", -2015),
    NOT_ENOUGH_BALANCE("Not enough balnce", -2016),
    DUPLICATE_CLIENT_ORDER_ID("The clOrdId is already in use", -2017),
}
