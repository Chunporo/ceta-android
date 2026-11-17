package com.ceta.cinema.domain.model

import java.time.Instant
import java.time.LocalDate

/** Money is always whole VND as Long. */
enum class SeatStatus { AVAILABLE, HELD, HELD_BY_ME, SOLD, INACTIVE;
    companion object {
        // Anything the app does not know is unselectable, never guessed as free.
        fun parse(value: String): SeatStatus = entries.firstOrNull { it.name == value } ?: INACTIVE
    }
}

enum class SeatKind { STANDARD, VIP, COUPLE;
    companion object {
        fun parse(type: String?): SeatKind = when (type) {
            "VIP", "DELUXE" -> VIP
            "SWEETBOX" -> COUPLE
            else -> STANDARD
        }
    }
}

data class Seat(
    val id: Long, val label: String, val row: Int, val column: Int,
    val kind: SeatKind, val status: SeatStatus, val price: Long,
) {
    val selectable: Boolean get() = status == SeatStatus.AVAILABLE
}

enum class BookingStatus { PENDING, PAID, CANCELLED, EXPIRED, REFUNDED, UNKNOWN;
    companion object {
        fun parse(value: String): BookingStatus = entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

enum class ShowFormat(val label: String) { TWO_D("2D"), THREE_D("3D"), IMAX("IMAX");
    companion object {
        fun parse(value: String?): ShowFormat = entries.firstOrNull { it.name == value } ?: TWO_D
    }
}

enum class LanguageMode { SUB, DUB;
    companion object {
        fun parse(value: String?): LanguageMode = if (value == "DUB") DUB else SUB
    }
}

enum class MovieStatus { NOW_SHOWING, COMING_SOON, EARLY_ACCESS, UNKNOWN;
    companion object {
        fun parse(value: String?): MovieStatus = entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

data class Genre(val id: Long, val name: String, val slug: String)

data class Movie(
    val id: String, val title: String, val posterUrl: String?, val ageRating: String?, val status: MovieStatus,
    val durationMin: Int?, val releaseDate: LocalDate?, val trailerUrl: String?, val director: String?, val language: String?,
    val description: String?, val ratingAverage: Double?, val ratingCount: Long, val fromPrice: Long?,
    val cast: List<String>, val genres: List<String>, val formats: Set<ShowFormat>,
) {
    val isNowShowing: Boolean get() = status == MovieStatus.NOW_SHOWING || status == MovieStatus.EARLY_ACCESS
}

data class Cinema(
    val id: Long, val name: String, val city: String, val address: String,
    val latitude: Double?, val longitude: Double?, val imax: Boolean,
)

data class Showtime(
    val id: Long, val movieId: String?, val movieTitle: String?, val posterUrl: String?,
    val cinemaId: Long?, val cinemaName: String?, val roomName: String?,
    val start: Instant, val end: Instant?, val basePrice: Long, val format: ShowFormat, val languageMode: LanguageMode,
    val cancelled: Boolean,
)

data class Combo(val id: Long, val name: String, val nameEn: String?, val description: String?, val descriptionEn: String?, val imageUrl: String?, val price: Long) {
    fun title(english: Boolean) = if (english && !nameEn.isNullOrBlank()) nameEn else name
    fun detail(english: Boolean) = if (english && !descriptionEn.isNullOrBlank()) descriptionEn else description
}

enum class PromotionKind { PERCENT_OFF, FIXED_TICKET_PRICE, COUNTER_PERK }

data class Promotion(
    val id: Long, val code: String, val title: String, val titleEn: String?, val description: String?, val descriptionEn: String?,
    val imageUrl: String?, val kind: PromotionKind, val validUntil: Instant?,
) {
    fun title(english: Boolean) = if (english && !titleEn.isNullOrBlank()) titleEn else title
    fun detail(english: Boolean) = if (english && !descriptionEn.isNullOrBlank()) descriptionEn else description
    /** A counter perk is shown but cannot be typed into checkout. */
    val applicableOnline: Boolean get() = kind != PromotionKind.COUNTER_PERK
}

data class Review(val id: Long, val rating: Int, val comment: String?, val author: String, val mine: Boolean, val createdAt: Instant?)

data class Loyalty(
    val balance: Long, val lifetimeEarned: Long, val tier: String, val nextTier: String?, val pointsToNextTier: Long,
    val redeemBlockPoints: Int, val redeemBlockVnd: Long, val ticketsWatched: Long, val vouchers: Long, val reviews: Long,
) {
    val canRedeem: Boolean get() = balance >= redeemBlockPoints
}

data class BookingSeat(val seatId: Long, val label: String, val price: Long)
data class ComboLine(val comboId: Long, val name: String, val quantity: Int, val unitPrice: Long) {
    val total: Long get() = quantity * unitPrice
}
data class BookingTicketRef(val ticketId: Long, val seatId: Long, val label: String, val code: String)

data class Booking(
    val id: Long, val status: BookingStatus, val totalAmount: Long,
    val holdExpiresAt: Instant?, val secondsRemaining: Long,
    val seats: List<BookingSeat>, val tickets: List<BookingTicketRef>,
    val movieTitle: String?, val roomName: String?, val startTime: Instant?, val refundRequired: Boolean,
    val comboAmount: Long, val combos: List<ComboLine>, val discountAmount: Long, val voucherCode: String?,
    val pointsAmount: Long, val pointsUsed: Int,
) {
    val seatsAmount: Long get() = seats.sumOf { it.price }
}

data class CreatedBooking(val bookingId: Long, val holdExpiresAt: Instant?, val totalAmount: Long, val seats: List<BookingSeat>)

data class Extras(
    val totalAmount: Long, val comboAmount: Long, val discountAmount: Long, val voucherCode: String?,
    val combos: List<ComboLine>, val pointsAmount: Long, val pointsUsed: Int,
)

data class BookingSummary(
    val id: Long, val status: BookingStatus, val totalAmount: Long, val movieTitle: String?, val posterUrl: String?,
    val roomName: String?, val seatNames: List<String>, val startTime: Instant?, val createdAt: Instant?,
)

data class Ticket(
    val id: Long, val bookingId: Long, val code: String, val seatLabel: String, val price: Long, val qrPayload: String, val used: Boolean,
    val showtimeId: Long, val movieTitle: String, val posterUrl: String?, val cinemaName: String?, val cinemaAddress: String?,
    val roomName: String, val format: ShowFormat, val start: Instant, val end: Instant?,
)

data class AppNotification(
    val id: Long, val type: String, val title: String, val body: String, val bookingId: Long?, val movieId: String?,
    val createdAt: Instant?, val read: Boolean,
)
