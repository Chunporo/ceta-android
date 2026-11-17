package com.ceta.cinema.data.remote

import com.ceta.cinema.core.SessionEvents
import com.ceta.cinema.data.local.secure.TokenStore
import com.ceta.cinema.data.remote.api.CetaApi
import com.ceta.cinema.data.remote.api.RefreshApi
import com.ceta.cinema.data.remote.interceptor.AuthInterceptor
import com.ceta.cinema.data.remote.interceptor.ErrorInterceptor
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** One place that wires the clients, shared by Hilt and the MockWebServer tests. */
object NetworkFactory {
    fun create(baseUrl: String, tokens: TokenStore, events: SessionEvents, gson: Gson, debug: Boolean): CetaApi {
        fun retrofit(client: OkHttpClient) = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val plain = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
        val refreshApi = retrofit(plain).create(RefreshApi::class.java)

        val client = plain.newBuilder()
            .addInterceptor(ErrorInterceptor())                                 // outermost: sees what AuthInterceptor returns
            .addInterceptor(AuthInterceptor(tokens, refreshApi, events))
            .apply {
                if (debug) {
                    // BASIC logs the request line and status only: no headers, no bodies, so no tokens or payments.
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()
        return retrofit(client).create(CetaApi::class.java)
    }
}
