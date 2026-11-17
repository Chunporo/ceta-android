package com.ceta.cinema.data.repository

import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.safeApiCall
import com.ceta.cinema.data.remote.api.CetaApi
import com.ceta.cinema.data.remote.dto.CreateBookingRequest
import com.ceta.cinema.data.remote.mapper.toDomain
import com.ceta.cinema.domain.model.Booking
import com.ceta.cinema.domain.model.CreatedBooking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(private val api: CetaApi) {
    suspend fun create(showtimeId: Long, seatIds: List<Long>): ApiResult<CreatedBooking> =
        safeApiCall { api.createBooking(CreateBookingRequest(showtimeId, seatIds)).toDomain() }

    suspend fun get(id: Long): ApiResult<Booking> = safeApiCall { api.booking(id).toDomain() }

    suspend fun cancel(id: Long): ApiResult<Unit> = safeApiCall { api.cancelBooking(id) }
}
