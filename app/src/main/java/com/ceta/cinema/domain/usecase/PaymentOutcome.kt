package com.ceta.cinema.domain.usecase

import com.ceta.cinema.domain.model.Booking
import com.ceta.cinema.domain.model.BookingStatus

/** What the payment result screen shows. Only the backend's booking status decides this, never what the VNPay tab did. */
enum class PaymentOutcome {
    /** Still waiting for the confirmation to arrive. */
    PROCESSING,
    PAID,
    /** The hold ran out and nothing was paid. */
    EXPIRED,
    CANCELLED,
    /** Money came in after the seats were gone, or the order was refunded: the customer is owed their money back. */
    REFUND,
    /** Waited a minute with no answer: stop spinning and let the customer check again. */
    STILL_PENDING;

    val terminal: Boolean get() = this != PROCESSING && this != STILL_PENDING
}

object PaymentPolling {
    /** Seconds between checks: 2, 3, 5, 8, then 8 again, for about a minute in all. */
    val DELAYS_SECONDS: List<Long> = listOf(2, 3, 5, 8, 8, 8, 8, 8, 8)

    fun outcome(booking: Booking): PaymentOutcome = when (booking.status) {
        BookingStatus.PAID -> PaymentOutcome.PAID
        BookingStatus.REFUNDED -> PaymentOutcome.REFUND
        BookingStatus.EXPIRED -> if (booking.refundRequired) PaymentOutcome.REFUND else PaymentOutcome.EXPIRED
        BookingStatus.CANCELLED -> PaymentOutcome.CANCELLED
        BookingStatus.PENDING, BookingStatus.UNKNOWN -> PaymentOutcome.PROCESSING
    }
}
