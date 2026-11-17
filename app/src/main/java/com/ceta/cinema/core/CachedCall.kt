package com.ceta.cinema.core

/**
 * Serve a fresh cached value, else fetch. A failed fetch (network only) falls back to the old value and raises [OfflineFlag];
 * a failed fetch with nothing cached is an error. Never use this for the seat map.
 */
suspend fun <K : Any, V : Any> cachedCall(cache: TtlCache<K, V>, key: K, offline: OfflineFlag, fetch: suspend () -> V): ApiResult<V> {
    cache.fresh(key)?.let { return ApiResult.Ok(it) }
    return when (val result = safeApiCall(fetch)) {
        is ApiResult.Ok -> {
            cache.put(key, result.data)
            offline.set(false)
            result
        }
        is ApiResult.Err -> {
            val old = cache.stale(key)
            if (old != null && result.error.code == ApiError.NETWORK) {
                offline.set(true)
                ApiResult.Ok(old)
            } else result
        }
    }
}
