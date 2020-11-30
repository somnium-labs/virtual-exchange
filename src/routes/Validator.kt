package com.linebizplus.exchange.virtual.routes

import com.linebizplus.exchange.virtual.classification.Error
import com.linebizplus.exchange.virtual.exceptions.CommonException
import com.linebizplus.exchange.virtual.model.Member
import com.linebizplus.exchange.virtual.services.MemberService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*

fun validateParameter(queryParameters: Parameters, parameter: String): String {
    return queryParameters[parameter] ?: throw CommonException(Error.REQUIRED_PARAMETER)
}

fun validateMember(apiKey: String?): Member {
    apiKey ?: throw CommonException(Error.INVALID_API_KEY)
    return MemberService.getMemberByApiKey(apiKey) ?: throw CommonException(Error.INVALID_API_KEY)
}

suspend fun errorRespond(call: ApplicationCall, error: Error) {
    val httpStatusCode = when (error) {
        Error.INVALID_API_KEY, Error.PERMISSION_DENIED -> HttpStatusCode.Unauthorized
        else -> HttpStatusCode.BadRequest
    }

    call.respond(httpStatusCode, object {
        val code = error.code
        val message = error.message
    })
}
