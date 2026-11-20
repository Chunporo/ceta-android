package com.ceta.cinema.data.remote.dto

/** {@code voucherCode} and {@code combos} are optional; the backend ignores a null/empty one. */
data class CreateBookingRequest(
    val showtimeId: Long, val seatIds: List<Long>,
    val combos: List<ComboLineRequest>? = null, val voucherCode: String? = null,
)

data class ComboLineRequest(val comboId: Long, val quantity: Int)

data class BookingSeatDto(val seatId: Long, val label: String, val price: Long)
data class ComboLineDto(val comboId: Long, val name: String, val quantity: Int, val unitPrice: Long)

data class CreateBookingDto(
    val bookingId: Long,
    val holdExpiresAt: String?,
    val totalAmount: Long,
    val seats: List<BookingSeatDto> = emptyList(),
    val comboAmount: Long = 0,
    val combos: List<ComboLineDto> = emptyList(),
    val discountAmount: Long = 0,
    val voucherCode: String? = null,
)

data class BookingTicketDto(val ticketId: Long, val seatId: Long, val label: String, val code: String, val price: Long)

/** total = seats + comboAmount - discountAmount - pointsAmount. */
data class BookingDto(
    val id: Long,
    val status: String,                 // PENDING | PAID | CANCELLED | EXPIRED | REFUNDED
    val holdExpiresAt: String?,         // ISO-8601 UTC, null once nothing is held
    val secondsRemaining: Long = 0,
    val totalAmount: Long,
    val seats: List<BookingSeatDto> = emptyList(),
    val tickets: List<BookingTicketDto> = emptyList(),
    val movieTitle: String? = null,
    val roomName: String? = null,
    val startTime: String? = null,
    val refundRequired: Boolean = false,
    val comboAmount: Long = 0,
    val combos: List<ComboLineDto> = emptyList(),
    val discountAmount: Long = 0,
    val voucherCode: String? = null,
    val pointsAmount: Long = 0,
    val pointsUsed: Int = 0,
    val showtimeId: Long? = null,
)

/** The full desired state of the order's extras (snacks, voucher, points); see PUT /bookings/{id}/extras. */
data class BookingExtrasRequest(val combos: List<ComboLineRequest>?, val voucherCode: String?, val usePoints: Boolean)

data class BookingExtrasDto(
    val bookingId: Long, val totalAmount: Long, val comboAmount: Long, val discountAmount: Long,
    val voucherCode: String? = null, val combos: List<ComboLineDto> = emptyList(),
    val pointsAmount: Long = 0, val pointsUsed: Int = 0,
)

data class BookingHistoryDto(
    val id: Long, val status: String, val totalAmount: Long, val createdAt: String? = null, val showtimeId: Long? = null,
    val startTime: String? = null, val movieTitle: String? = null, val moviePosterUrl: String? = null,
    val roomName: String? = null, val seatNames: List<String> = emptyList(),
)

/** POST /bookings/{id}/cancel on a PAID booking: it becomes REFUNDED and the money goes through the refund queue. */
data class CancelPaidDto(val bookingId: Long, val status: String, val refundAmount: Long = 0)

data class PaymentUrlRequest(val bookingId: Long, val provider: String? = null)
data class PaymentMethodDto(val code: String, val enabled: Boolean = false)
data class PaymentUrlDto(val payUrl: String, val txnRef: String)

data class TicketShowtimeDto(
    val showtimeId: Long,
    val startTime: String,
    val endTime: String? = null,
    val movieTitle: String,
    val roomName: String,
    val cinemaName: String? = null,
    val cinemaAddress: String? = null,
    val format: String? = null,
    val posterUrl: String? = null,
    val ageRating: String? = null,
)

data class TicketDto(
    val ticketId: Long,
    val bookingId: Long,
    val code: String,
    val seatId: Long,
    val seatLabel: String,
    val price: Long,
    val showtime: TicketShowtimeDto,
    val qrPayload: String,
    val used: Boolean = false,
)

data class PageDto<T>(val items: List<T>, val page: Int, val size: Int, val totalItems: Long, val totalPages: Int)
