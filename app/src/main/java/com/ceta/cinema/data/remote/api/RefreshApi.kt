package com.ceta.cinema.data.remote.api

import com.ceta.cinema.data.remote.dto.RefreshRequest
import com.ceta.cinema.data.remote.dto.TokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/** Used only by AuthInterceptor, on a client without interceptors, so a refresh can never trigger a refresh. */
interface RefreshApi {
    @POST("auth/refresh")
    fun refresh(@Body body: RefreshRequest): Call<TokenResponse>
}
