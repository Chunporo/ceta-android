package com.ceta.cinema.data.repository

import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.safeApiCall
import com.ceta.cinema.data.remote.api.CetaApi
import com.ceta.cinema.data.remote.dto.ReviewRequest
import com.ceta.cinema.data.remote.mapper.toDomain
import com.ceta.cinema.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

/** Everything that belongs to the signed-in customer and is never cached: points, wallet, reviews, reminders, notifications. */
@Singleton
class AccountRepository @Inject constructor(private val api: CetaApi) {
    suspend fun loyalty(): ApiResult<Loyalty> = safeApiCall { api.loyalty().toDomain() }

    suspend fun walletPromotionIds(): ApiResult<Set<Long>> = safeApiCall { api.vouchers().map { it.promotion.id }.toSet() }
    suspend fun wallet(): ApiResult<List<Promotion>> = safeApiCall { api.vouchers().map { it.promotion.toDomain() } }
    suspend fun saveVoucher(promotionId: Long): ApiResult<Unit> = safeApiCall { api.saveVoucher(promotionId); Unit }
    suspend fun removeVoucher(promotionId: Long): ApiResult<Unit> = safeApiCall { api.removeVoucher(promotionId) }

    suspend fun reviews(movieId: String, page: Int = 0): ApiResult<List<Review>> = safeApiCall { api.reviews(movieId, page).items.map { it.toDomain() } }
    suspend fun myReview(movieId: String): ApiResult<Review> = safeApiCall { api.myReview(movieId).toDomain() }
    suspend fun saveReview(movieId: String, rating: Int, comment: String?): ApiResult<Review> =
        safeApiCall { api.saveReview(movieId, ReviewRequest(rating, comment)).toDomain() }
    suspend fun deleteReview(movieId: String): ApiResult<Unit> = safeApiCall { api.deleteReview(movieId) }

    suspend fun reminderMovieIds(): ApiResult<Set<String>> = safeApiCall { api.reminders().map { it.id }.toSet() }
    suspend fun addReminder(movieId: String): ApiResult<Unit> = safeApiCall { api.addReminder(movieId) }
    suspend fun removeReminder(movieId: String): ApiResult<Unit> = safeApiCall { api.removeReminder(movieId) }

    suspend fun notifications(page: Int = 0): ApiResult<List<AppNotification>> = safeApiCall { api.notifications(page).items.map { it.toDomain() } }
    suspend fun unreadCount(): ApiResult<Long> = safeApiCall { api.unreadCount().count }
    suspend fun markRead(id: Long): ApiResult<Unit> = safeApiCall { api.markRead(id) }
    suspend fun markAllRead(): ApiResult<Unit> = safeApiCall { api.markAllRead() }
}
