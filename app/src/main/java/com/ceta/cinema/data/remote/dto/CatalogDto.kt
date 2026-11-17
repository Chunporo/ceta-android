package com.ceta.cinema.data.remote.dto

/** Movie ids are UUID strings on the backend, showtime and seat ids are Long. */
data class GenreDto(val id: String? = null, val name: String? = null, val slug: String? = null)

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
    val description: String? = null,
    val ratingScore: Double? = null,
    val cast: List<String> = emptyList(),
    val formats: List<String> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
)

data class MovieSummaryDto(val id: String, val title: String, val slug: String? = null, val posterUrl: String? = null)
data class RoomSummaryDto(val id: Long? = null, val name: String? = null)

data class ShowtimeDto(
    val id: Long,
    val movie: MovieSummaryDto? = null,
    val room: RoomSummaryDto? = null,
    val startTime: String,
    val endTime: String? = null,
    val basePrice: Long? = null,
    val status: String? = null,
)

data class SeatDto(
    val seatId: Long,
    val label: String,
    val rowNumber: Int? = null,
    val columnNumber: Int? = null,
    val type: String? = null,
    val active: Boolean = true,
    val status: String,                 // AVAILABLE | HELD | HELD_BY_ME | SOLD | INACTIVE
    val price: Long? = null,
    val holdExpiresAt: String? = null,
)
