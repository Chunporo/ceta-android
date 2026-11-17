package com.ceta.cinema.data.remote.interceptor

import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiException
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Interceptor
import okhttp3.Response

/** Outermost interceptor: any non-2xx answer becomes an [ApiException] carrying the parsed `{code,message,details}`. */
class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) return response
        val body = response.body?.string()
        response.close()
        throw ApiException(parse(response.code, body))
    }

    internal fun parse(status: Int, body: String?): ApiError {
        val json = runCatching { JsonParser.parseString(body).asJsonObject }.getOrNull()
        var code = json?.get("code")?.takeIf { it.isJsonPrimitive }?.asString
        if (code == null) code = if (status == 401) ApiError.UNAUTHORIZED else ApiError.UNKNOWN
        val seatIds = json?.getAsJsonArrayOrNull("details")?.toList().orEmpty()
            .mapNotNull { runCatching { it.asJsonObject.get("seatId").asLong }.getOrNull() }
        val message = json?.get("message")?.takeIf { it.isJsonPrimitive }?.asString
        return ApiError(code, status, seatIds, message)
    }

    private fun JsonObject.getAsJsonArrayOrNull(name: String) =
        get(name)?.takeIf { it.isJsonArray }?.asJsonArray
}
