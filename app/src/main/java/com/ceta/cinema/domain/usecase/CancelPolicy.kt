package com.ceta.cinema.domain.usecase

import java.time.Duration
import java.time.Instant

/**
 * When a paid ticket can be cancelled and what comes back (design item 9). Mirrors the backend (D-15):
 * cancel until 60 minutes before the show, never once a ticket is scanned; the whole order is refunded in cash
 * through the refund queue (an admin pays it back by hand), so nothing is credited as points.
 * Keep MIN_BEFORE_SHOW equal to the backend's `app.booking.cancel-before`.
 */
object CancelPolicy {
    val MIN_BEFORE_SHOW: Duration = Duration.ofMinutes(60)

    fun canCancel(start: Instant, now: Instant, anyTicketUsed: Boolean): Boolean =
        !anyTicketUsed && !now.plus(MIN_BEFORE_SHOW).isAfter(start)

    /** The refund is the whole order. */
    fun refundFor(total: Long): Long = total
}
