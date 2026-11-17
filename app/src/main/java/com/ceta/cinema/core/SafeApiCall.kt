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
} catch (e: IllegalStateException) {
    // Retrofit's Gson converter reads the adapter directly, so a reply of the wrong shape (an object where a list was
    // expected) surfaces as a bare IllegalStateException rather than a JsonParseException.
    ApiResult.Err(ApiError(ApiError.UNKNOWN, 0, message = "Unexpected response shape"))
} catch (e: NumberFormatException) {
    ApiResult.Err(ApiError(ApiError.UNKNOWN, 0, message = "Unreadable number"))
} catch (e: IOException) {
    ApiResult.Err(ApiError(ApiError.NETWORK, 0, message = e.javaClass.simpleName))
}
