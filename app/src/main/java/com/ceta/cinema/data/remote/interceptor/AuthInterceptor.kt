package com.ceta.cinema.data.remote.interceptor

import com.ceta.cinema.core.SessionEvents
import com.ceta.cinema.data.local.secure.TokenStore
import com.ceta.cinema.data.remote.api.RefreshApi
import com.ceta.cinema.data.remote.dto.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Adds the bearer token. On a 401 it refreshes once and retries; the lock makes concurrent requests share a
 * single refresh (the first one rotates the token, the others see the new token and just retry).
 */
class AuthInterceptor(
    private val tokens: TokenStore,
    private val refreshApi: RefreshApi,
    private val events: SessionEvents,
) : Interceptor {
    private val lock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (isAuthEndpoint(original)) return chain.proceed(original)

        val used = runBlocking { tokens.access() }
        val response = chain.proceed(original.withToken(used))
        if (response.code != 401 || used == null) return response

        val fresh = synchronized(lock) { refreshedToken(stale = used) } ?: return response
        response.close()
        return chain.proceed(original.withToken(fresh))
    }

    private fun refreshedToken(stale: String): String? {
        val current = runBlocking { tokens.access() }
        if (current != null && current != stale) return current // another request already refreshed

        val refresh = runBlocking { tokens.refresh() }
        if (refresh == null) {
            expire()
            return null
        }
        val result = refreshApi.refresh(RefreshRequest(refresh)).execute() // IOException: network, keep the session
        val body = result.body()
        if (result.isSuccessful && body != null) {
            runBlocking { tokens.saveTokens(body.accessToken, body.refreshToken) }
            return body.accessToken
        }
        if (result.code() in 400..403) {
            expire()
            return null
        }
        throw IOException("Token refresh failed with HTTP ${result.code()}")
    }

    private fun expire() {
        runBlocking { tokens.clear() }
        events.notifyExpired()
    }

    private fun Request.withToken(token: String?): Request =
        if (token == null) this else newBuilder().header("Authorization", "Bearer $token").build()

    private fun isAuthEndpoint(request: Request): Boolean {
        val path = request.url.encodedPath
        return PUBLIC_AUTH_PATHS.any { path.endsWith(it) }
    }

    private companion object {
        val PUBLIC_AUTH_PATHS = listOf(
            "/auth/login", "/auth/register", "/auth/refresh", "/auth/firebase",
            "/auth/forgot-password", "/auth/reset-password",
        )
    }
}
