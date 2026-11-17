package com.ceta.cinema.core

import java.io.IOException

sealed interface ApiResult<out T> {
    data class Ok<T>(val data: T) : ApiResult<T>
    data class Err(val error: ApiError) : ApiResult<Nothing>
}

data class ApiError(
    val code: String,                       // SEAT_CONFLICT, HOLD_EXPIRED, ... (BASELINE section 7)
    val httpStatus: Int,                    // 0 when there was no HTTP response
    val conflictSeatIds: List<Long> = emptyList(),
    val message: String? = null,            // for logs only, never shown to the customer
) {
    companion object {
        const val NETWORK = "NETWORK"
        const val UNAUTHORIZED = "UNAUTHORIZED"
        const val UNKNOWN = "UNKNOWN"
    }
}

/** Thrown by the error interceptor for every non-2xx answer; it is an IOException so OkHttp lets it through. */
class ApiException(val error: ApiError) : IOException(error.code)
