package com.ceta.cinema.data.remote.mapper

import com.ceta.cinema.data.remote.dto.*
import com.ceta.cinema.domain.model.*
import java.time.Instant

/** The only place that parses timestamps: if the backend ever sends epoch millis, fix it here. */
internal fun String?.toInstantOrNull(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }

fun SeatDto.toDomain() = Seat(
    id = seatId, label = label, row = rowNumber ?: 0, column = columnNumber ?: 0,
    type = type ?: "STANDARD",
    status = if (active) SeatStatus.parse(status) else SeatStatus.INACTIVE,
    price = price ?: 0L,
)

fun BookingSeatDto.toDomain() = BookingSeat(seatId, label, price)

fun CreateBookingDto.toDomain() = CreatedBooking(bookingId, holdExpiresAt.toInstantOrNull(), totalAmount, seats.map { it.toDomain() })

fun BookingDto.toDomain() = Booking(
    id = id, status = BookingStatus.parse(status), totalAmount = totalAmount,
    holdExpiresAt = holdExpiresAt.toInstantOrNull(), secondsRemaining = secondsRemaining,
    seats = seats.map { it.toDomain() }, ticketCount = tickets.size,
    movieTitle = movieTitle, roomName = roomName, startTime = startTime.toInstantOrNull(), refundRequired = refundRequired,
)
