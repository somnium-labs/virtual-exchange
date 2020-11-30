package com.linebizplus.exchange.mock.exceptions

import com.linebizplus.exchange.mock.classification.Error

class CommonException(val error: Error) : Exception()
