package com.ceta.cinema.domain.model

import java.time.Instant

/** Money is always whole VND as Long. */
enum class SeatStatus { AVAILABLE, HELD, HELD_BY_ME, SOLD, INACTIVE;
    companion object {
        // Anything the app does not know is unselectable, never guessed as free.
        fun parse(value: String): SeatStatus = entries.firstOrNull { it.name == value } ?: INACTIVE
    }
}

data class Seat(
    val id: Long, val label: String, val row: Int, val column: Int,
    val type: String, val status: SeatStatus, val price: Long,
) {
    val selectable: Boolean get() = status == SeatStatus.AVAILABLE
}

enum class BookingStatus { PENDING, PAID, CANCELLED, EXPIRED, REFUNDED, UNKNOWN;
    companion object {
        fun parse(value: String): BookingStatus = entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

data class BookingSeat(val seatId: Long, val label: String, val price: Long)

data class Booking(
    val id: Long, val status: BookingStatus, val totalAmount: Long,
    val holdExpiresAt: Instant?, val secondsRemaining: Long,
    val seats: List<BookingSeat>, val ticketCount: Int,
    val movieTitle: String?, val roomName: String?, val startTime: Instant?, val refundRequired: Boolean,
)

data class CreatedBooking(val bookingId: Long, val holdExpiresAt: Instant?, val totalAmount: Long, val seats: List<BookingSeat>)
