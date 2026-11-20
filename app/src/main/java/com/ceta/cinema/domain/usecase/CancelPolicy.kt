package com.ceta.cinema.domain.usecase

import java.time.Duration
import java.time.Instant

/**
 * When a paid ticket can be cancelled and what comes back (design item 9).
 *
 * TODO(business): the design's rule is an assumption: cancel until 2 hours before the show, refund 100 % as CETA points
 * (1 point = 1 dong). The backend today allows a cancel until 60 minutes before and queues a refund for an admin to pay by hand;
 * it does not credit points. Confirm the policy, then change these numbers (and the backend) together.
 */
object CancelPolicy {
    val MIN_BEFORE_SHOW: Duration = Duration.ofHours(2)

    fun canCancel(start: Instant, now: Instant, anyTicketUsed: Boolean): Boolean =
        !anyTicketUsed && !now.plus(MIN_BEFORE_SHOW).isAfter(start)

    /** The refund is the whole order. */
    fun refundFor(total: Long): Long = total
}
