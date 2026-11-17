package com.ceta.cinema.data.remote.dto

data class CreateBookingRequest(val showtimeId: Long, val seatIds: List<Long>)

data class BookingSeatDto(val seatId: Long, val label: String, val price: Long)

data class CreateBookingDto(
    val bookingId: Long,
    val holdExpiresAt: String?,
    val totalAmount: Long,
    val seats: List<BookingSeatDto> = emptyList(),
)

data class BookingTicketDto(val ticketId: Long, val seatId: Long, val label: String, val code: String, val price: Long)

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
)

data class PaymentUrlRequest(val bookingId: Long)
data class PaymentUrlDto(val payUrl: String, val txnRef: String)

data class TicketShowtimeDto(
    val showtimeId: Long,
    val startTime: String,
    val endTime: String? = null,
    val movieTitle: String,
    val roomName: String,
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
)

data class PageDto<T>(val items: List<T>, val page: Int, val size: Int, val totalItems: Long, val totalPages: Int)
