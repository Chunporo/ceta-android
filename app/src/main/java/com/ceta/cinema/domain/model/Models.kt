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

/** An actor: the character they play and a profile photo are optional. */
data class CastMember(val name: String, val role: String?, val photoUrl: String?)

/** A score another site gives the film, shown beside (never mixed into) the customers' score. */
data class ExternalRating(val source: Source, val value: Double, val scale: Int, val votes: Long?) {
    enum class Source(val label: String) { IMDB("IMDb"), ROTTEN_TOMATOES("Rotten Tomatoes"), METACRITIC("Metacritic"), TMDB("TMDB") }

    /** 7.4 for the sites out of 10, 87% for Rotten Tomatoes, 71 for Metacritic. */
    val display: String get() = when {
        source == Source.ROTTEN_TOMATOES -> "${value.toInt()}%"
        scale == 100 -> value.toInt().toString()
        else -> "%.1f".format(java.util.Locale.ROOT, value)
    }
}

data class Movie(
    val id: String, val title: String, val posterUrl: String?, val ageRating: String?, val status: MovieStatus,
    val durationMin: Int?, val releaseDate: LocalDate?, val trailerUrl: String?, val director: String?, val language: String?,
    val description: String?, val ratingAverage: Double?, val ratingCount: Long, val fromPrice: Long?,
    val cast: List<String>, val genres: List<String>, val formats: Set<ShowFormat>,
    val castMembers: List<CastMember> = cast.map { CastMember(it, null, null) },
    val directorPhotoUrl: String? = null,
    /** A landscape still for the header of the movie page; the poster is used when there is none. */
    val backdropUrl: String? = null,
    val externalRatings: List<ExternalRating> = emptyList(),
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
    /** Free and sellable seats as the server counts them; null when it does not say (no inventory yet). */
    val availableSeats: Int? = null, val totalSeats: Int? = null,
) {
    val full: Boolean get() = availableSeats == 0

    /** How much of the room is taken, 0..1, for the availability bar; null when unknown. */
    val filled: Float? get() = if (availableSeats != null && totalSeats != null && totalSeats > 0) 1f - availableSeats.toFloat() / totalSeats else null
}

data class Combo(val id: Long, val name: String, val nameEn: String?, val description: String?, val descriptionEn: String?, val imageUrl: String?, val price: Long, val code: String? = null) {
    fun title(english: Boolean) = if (english && !nameEn.isNullOrBlank()) nameEn else name
    fun detail(english: Boolean) = if (english && !descriptionEn.isNullOrBlank()) descriptionEn else description
}

enum class PromotionKind { PERCENT_OFF, FIXED_TICKET_PRICE, COUNTER_PERK }

data class Promotion(
    val id: Long, val code: String, val title: String, val titleEn: String?, val description: String?, val descriptionEn: String?,
    val imageUrl: String?, val kind: PromotionKind, val validUntil: Instant?,
    val percentOff: Int? = null, val fixedPrice: Long? = null, val formats: List<String> = emptyList(),
) {
    /**
     * The big word on the banner: 50K, -20%, 49K, IMAX.
     * TODO(backend): a `highlight` field on the promotion; until then it is worked out from the rules.
     */
    val highlight: String? get() = when {
        kind == PromotionKind.PERCENT_OFF && percentOff != null -> "−$percentOff%"
        kind == PromotionKind.FIXED_TICKET_PRICE && fixedPrice != null -> "${fixedPrice / 1000}K"
        "IMAX" in formats -> "IMAX"
        else -> null
    }

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
    val isTopTier: Boolean get() = nextTier == null

    /** How far along the way to the next tier (0..1), for the progress bar. */
    val progressToNextTier: Float get() = if (isTopTier) 1f else (lifetimeEarned.toFloat() / (lifetimeEarned + pointsToNextTier).coerceAtLeast(1)).coerceIn(0f, 1f)
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
    /** The show this order is for; null from an older server. */
    val showtimeId: Long? = null,
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
    val ageRating: String? = null,
)

data class AppNotification(
    val id: Long, val type: String, val title: String, val body: String, val bookingId: Long?, val movieId: String?,
    val createdAt: Instant?, val read: Boolean,
)

/** Who the signed-in customer is. A phone sign-in has no real email, so it carries the phone number instead. */
data class Profile(val username: String, val email: String?, val phone: String?)

/** A coming-soon movie the customer asked to be told about. */
data class ReminderMovie(val id: String, val title: String, val posterUrl: String?)

/** The ways to pay. The server says which are switched on; VNPay is always there. */
enum class PaymentProvider(val code: String) {
    VNPAY("VNPAY"), MOMO("MOMO"), VIETQR("VIETQR");

    companion object {
        fun parse(code: String): PaymentProvider? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
