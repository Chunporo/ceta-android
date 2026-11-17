package com.ceta.cinema.data.remote.api

import com.ceta.cinema.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Paths are relative to API_BASE_URL (".../api/v1/"). Shapes follow the backend, not the draft spec. */
interface CetaApi {
    @POST("auth/firebase") suspend fun loginWithFirebase(@Body body: FirebaseLoginRequest): TokenResponse
    @POST("auth/logout") suspend fun logout(@Body body: RefreshRequest)

    @GET("movies")
    suspend fun movies(
        @Query("q") q: String? = null,
        @Query("genre") genre: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int? = null,
        @Query("size") size: Int = 20,
    ): List<MovieDto>

    @GET("movies/{id}") suspend fun movie(@Path("id") id: String): MovieDto

    @GET("showtimes")
    suspend fun showtimes(@Query("movieId") movieId: String, @Query("date") date: String): List<ShowtimeDto>

    @GET("showtimes/{id}/seats") suspend fun seats(@Path("id") showtimeId: Long): List<SeatDto>

    @POST("bookings") suspend fun createBooking(@Body body: CreateBookingRequest): CreateBookingDto
    @GET("bookings/{id}") suspend fun booking(@Path("id") id: Long): BookingDto
    @DELETE("bookings/{id}") suspend fun cancelBooking(@Path("id") id: Long)

    @POST("payments/vnpay/create-url") suspend fun createPaymentUrl(@Body body: PaymentUrlRequest): PaymentUrlDto

    @GET("tickets/my") suspend fun myTickets(@Query("page") page: Int = 0, @Query("size") size: Int = 20): PageDto<TicketDto>
    @GET("tickets/{id}") suspend fun ticket(@Path("id") id: Long): TicketDto
}
