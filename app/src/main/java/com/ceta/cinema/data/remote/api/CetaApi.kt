package com.ceta.cinema.data.remote.api

import com.ceta.cinema.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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
    @GET("genres") suspend fun genres(): List<GenreDto>

    @GET("movies/{id}/reviews") suspend fun reviews(@Path("id") movieId: String, @Query("page") page: Int = 0, @Query("size") size: Int = 20): PageDto<ReviewDto>
    @GET("movies/{id}/review") suspend fun myReview(@Path("id") movieId: String): ReviewDto
    @PUT("movies/{id}/review") suspend fun saveReview(@Path("id") movieId: String, @Body body: ReviewRequest): ReviewDto
    @DELETE("movies/{id}/review") suspend fun deleteReview(@Path("id") movieId: String)

    @PUT("movies/{id}/reminder") suspend fun addReminder(@Path("id") movieId: String)
    @DELETE("movies/{id}/reminder") suspend fun removeReminder(@Path("id") movieId: String)
    @GET("reminders") suspend fun reminders(): List<MovieSummaryDto>

    @GET("cinemas") suspend fun cinemas(@Query("city") city: String? = null): List<CinemaDto>

    @GET("showtimes")
    suspend fun showtimes(
        @Query("movieId") movieId: String? = null,
        @Query("cinemaId") cinemaId: Long? = null,
        @Query("date") date: String? = null,
        @Query("format") format: String? = null,
    ): List<ShowtimeDto>

    @GET("showtimes/{id}") suspend fun showtime(@Path("id") id: Long): ShowtimeDto

    @GET("combos") suspend fun combos(): List<ComboDto>
    @GET("promotions") suspend fun promotions(): List<PromotionDto>
    @GET("vouchers") suspend fun vouchers(): List<VoucherDto>
    @PUT("vouchers/{promotionId}") suspend fun saveVoucher(@Path("promotionId") id: Long): VoucherDto
    @DELETE("vouchers/{promotionId}") suspend fun removeVoucher(@Path("promotionId") id: Long)

    @GET("me/loyalty") suspend fun loyalty(): LoyaltyDto

    @GET("notifications") suspend fun notifications(@Query("page") page: Int = 0, @Query("size") size: Int = 30): PageDto<NotificationDto>
    @GET("notifications/unread-count") suspend fun unreadCount(): UnreadCountDto
    @PUT("notifications/{id}/read") suspend fun markRead(@Path("id") id: Long)
    @PUT("notifications/read-all") suspend fun markAllRead()

    @GET("showtimes/{id}/seats") suspend fun seats(@Path("id") showtimeId: Long): List<SeatDto>

    @POST("bookings") suspend fun createBooking(@Body body: CreateBookingRequest): CreateBookingDto
    @GET("bookings/{id}") suspend fun booking(@Path("id") id: Long): BookingDto
    @DELETE("bookings/{id}") suspend fun cancelBooking(@Path("id") id: Long)
    @PUT("bookings/{id}/extras") suspend fun updateExtras(@Path("id") id: Long, @Body body: BookingExtrasRequest): BookingExtrasDto
    @GET("bookings") suspend fun bookings(@Query("page") page: Int = 0, @Query("size") size: Int = 20): PageDto<BookingHistoryDto>

    @POST("payments/vnpay/create-url") suspend fun createPaymentUrl(@Body body: PaymentUrlRequest): PaymentUrlDto

    @GET("tickets/my") suspend fun myTickets(@Query("page") page: Int = 0, @Query("size") size: Int = 20): PageDto<TicketDto>
    @GET("tickets/{id}") suspend fun ticket(@Path("id") id: Long): TicketDto
}
