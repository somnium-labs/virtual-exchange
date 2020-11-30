package com.linebizplus.exchange.virtual.exceptions

import com.linebizplus.exchange.virtual.classification.Error

class CommonException(val error: Error) : Exception()
