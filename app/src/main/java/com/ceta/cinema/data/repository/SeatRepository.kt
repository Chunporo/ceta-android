package com.ceta.cinema.data.repository

import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.safeApiCall
import com.ceta.cinema.data.remote.api.CetaApi
import com.ceta.cinema.data.remote.mapper.toDomain
import com.ceta.cinema.domain.model.Seat
import javax.inject.Inject
import javax.inject.Singleton

/** The seat map is never cached: every call goes to the network (rule 3). */
@Singleton
class SeatRepository @Inject constructor(private val api: CetaApi) {
    suspend fun seats(showtimeId: Long): ApiResult<List<Seat>> =
        safeApiCall { api.seats(showtimeId).map { it.toDomain() } }
}
