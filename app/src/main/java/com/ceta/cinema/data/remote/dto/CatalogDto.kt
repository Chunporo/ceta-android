package com.ceta.cinema.data.remote.dto

/** Movie ids are UUID strings on the backend; showtime, seat, cinema and booking ids are Long. */
data class GenreDto(val id: Long? = null, val name: String? = null, val slug: String? = null)

data class MovieDto(
    val id: String,
    val title: String,
    val slug: String? = null,
    val posterUrl: String? = null,
    val ageRating: String? = null,
    val status: String? = null,
    val duration: Int? = null,
    val releaseDate: String? = null,
    val trailerUrl: String? = null,
    val director: String? = null,
    val language: String? = null,
    val description: String? = null,
    val ratingScore: Double? = null,
    val ratingAverage: Double? = null,       // 0..10 from customer reviews, null when nobody reviewed
    val ratingCount: Long = 0,
    val fromPrice: Long? = null,
    val cast: List<String> = emptyList(),
    val formats: List<String> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
)

data class MovieSummaryDto(val id: String, val title: String, val slug: String? = null, val posterUrl: String? = null)
data class RoomSummaryDto(val id: Long? = null, val code: String? = null, val name: String? = null)

data class CinemaDto(
    val id: Long,
    val code: String? = null,
    val name: String,
    val city: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imax: Boolean = false,
    val active: Boolean = true,
)

data class CinemaSummaryDto(val id: Long, val name: String, val city: String? = null, val address: String? = null)

data class ShowtimeDto(
    val id: Long,
    val movie: MovieSummaryDto? = null,
    val room: RoomSummaryDto? = null,
    val cinema: CinemaSummaryDto? = null,
    val startTime: String,
    val endTime: String? = null,
    val basePrice: Long? = null,
    val format: String? = null,             // TWO_D | THREE_D | IMAX
    val languageMode: String? = null,       // SUB | DUB
    val status: String? = null,             // SCHEDULED | CANCELLED
)

data class SeatDto(
    val seatId: Long,
    val label: String,
    val rowNumber: Int? = null,
    val columnNumber: Int? = null,
    val type: String? = null,
    val active: Boolean = true,
    val status: String,                     // AVAILABLE | HELD | HELD_BY_ME | SOLD | INACTIVE
    val price: Long? = null,
    val holdExpiresAt: String? = null,
)

data class ComboDto(
    val id: Long, val code: String? = null, val name: String, val nameEn: String? = null,
    val description: String? = null, val descriptionEn: String? = null, val imageUrl: String? = null,
    val price: Long, val active: Boolean = true, val sortOrder: Int = 0,
)

data class PromotionDto(
    val id: Long, val code: String, val title: String, val titleEn: String? = null,
    val description: String? = null, val descriptionEn: String? = null, val imageUrl: String? = null,
    val kind: String,                       // PERCENT_OFF | FIXED_TICKET_PRICE | COUNTER_PERK
    val percentOff: Int? = null, val maxDiscount: Long? = null, val fixedPrice: Long? = null,
    val weekdays: List<String> = emptyList(), val formats: List<String> = emptyList(),
    val firstOrderOnly: Boolean = false, val oncePerUser: Boolean = false,
    val validFrom: String? = null, val validUntil: String? = null, val active: Boolean = true, val sortOrder: Int = 0,
)

data class VoucherDto(val promotion: PromotionDto, val savedAt: String? = null)

data class ReviewDto(
    val id: Long, val rating: Int, val comment: String? = null, val author: String? = null,
    val mine: Boolean = false, val createdAt: String? = null, val updatedAt: String? = null,
)

data class ReviewRequest(val rating: Int, val comment: String?)

data class LoyaltyDto(
    val balance: Long, val lifetimeEarned: Long, val tier: String, val nextTier: String? = null, val pointsToNextTier: Long = 0,
    val redeemBlockPoints: Int = 2000, val redeemBlockVnd: Long = 20_000, val stats: LoyaltyStatsDto = LoyaltyStatsDto(),
)

data class LoyaltyStatsDto(val ticketsWatched: Long = 0, val vouchers: Long = 0, val reviews: Long = 0)

data class NotificationDto(
    val id: Long, val type: String, val title: String, val body: String, val bookingId: Long? = null,
    val createdAt: String? = null, val read: Boolean = false, val movieId: String? = null,
)

data class UnreadCountDto(val count: Long = 0)
