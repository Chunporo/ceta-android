package com.ceta.cinema.core

import com.google.gson.JsonParseException
import java.io.IOException

/** Turns the exceptions of one API call into an [ApiResult]; no screen ever sees a throwable. */
suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Ok(block())
} catch (e: ApiException) {
    ApiResult.Err(e.error)
} catch (e: JsonParseException) {
    ApiResult.Err(ApiError(ApiError.UNKNOWN, 0, message = "Unreadable response"))
} catch (e: IOException) {
    ApiResult.Err(ApiError(ApiError.NETWORK, 0, message = e.javaClass.simpleName))
}
