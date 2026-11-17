package com.ceta.cinema.data

import com.ceta.cinema.core.SessionEvents
import com.ceta.cinema.data.local.secure.TokenStore
import com.ceta.cinema.data.remote.NetworkFactory
import com.ceta.cinema.data.remote.api.CetaApi
import com.google.gson.Gson
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class FakeTokenStore(access: String? = null, refresh: String? = null) : TokenStore {
    @Volatile var accessToken: String? = access
    @Volatile var refreshToken: String? = refresh
    override suspend fun saveTokens(access: String, refresh: String) { accessToken = access; refreshToken = refresh }
    override suspend fun access() = accessToken
    override suspend fun refresh() = refreshToken
    override suspend fun clear() { accessToken = null; refreshToken = null }
}

fun fixture(name: String): String =
    object {}.javaClass.getResourceAsStream("/fixtures/$name")!!.bufferedReader().use { it.readText() }

fun json(body: String, code: Int = 200) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

fun apiFor(server: MockWebServer, tokens: TokenStore, events: SessionEvents = SessionEvents()): CetaApi =
    NetworkFactory.create(server.url("/api/v1/").toString(), tokens, events, com.ceta.cinema.data.remote.ApiGson.create(), debug = false)
