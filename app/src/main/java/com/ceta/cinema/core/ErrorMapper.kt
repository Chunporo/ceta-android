package com.ceta.cinema.core

import androidx.annotation.StringRes
import com.ceta.cinema.R

/** Maps an error code to the text the customer sees. The server's own message is never shown. */
object ErrorMapper {
    @StringRes
    fun messageFor(error: ApiError): Int = when {
        error.httpStatus == 401 || error.code == ApiError.UNAUTHORIZED -> R.string.error_session_expired
        else -> when (error.code) {
            "SEAT_CONFLICT" -> R.string.error_seat_conflict
            "BOOKING_ALREADY_PENDING" -> R.string.error_booking_already_pending
            "HOLD_EXPIRED" -> R.string.error_hold_expired
            "MAX_SEATS_EXCEEDED" -> R.string.error_max_seats
            "SHOWTIME_CLOSED" -> R.string.error_showtime_closed
            "SHOWTIME_CANCELLED" -> R.string.error_showtime_cancelled
            "INVALID_COUPLE_SEAT" -> R.string.error_invalid_couple_seat
            "BOOKING_NOT_CANCELLABLE" -> R.string.error_booking_not_cancellable
            "TOO_MANY_ATTEMPTS" -> R.string.error_too_many_attempts
            "TICKET_ALREADY_USED" -> R.string.error_ticket_used
            "HAS_ACTIVE_TICKETS" -> R.string.error_has_active_tickets
            "ACCOUNT_LOCKED" -> R.string.error_account_locked
            "EMAIL_NOT_VERIFIED" -> R.string.error_email_not_verified
            "VOUCHER_INVALID" -> R.string.ds_voucher_bad
            "VOUCHER_NOT_ELIGIBLE" -> R.string.error_voucher_not_eligible
            "COMBO_UNAVAILABLE" -> R.string.error_combo_unavailable
            "POINTS_NOT_APPLICABLE" -> R.string.error_points_not_applicable
            "INSUFFICIENT_POINTS" -> R.string.error_insufficient_points
            "PAYMENT_ALREADY_STARTED" -> R.string.error_payment_started
            "BOOKING_NOT_EDITABLE" -> R.string.error_booking_not_editable
            "REVIEW_NOT_ALLOWED" -> R.string.error_review_not_allowed
            "ALREADY_ON_SALE" -> R.string.error_already_on_sale
            AuthCodes.INVALID_CREDENTIALS -> R.string.error_invalid_credentials
            AuthCodes.EMAIL_IN_USE -> R.string.error_email_in_use
            AuthCodes.WEAK_PASSWORD -> R.string.error_weak_password
            AuthCodes.CANCELLED -> R.string.error_sign_in_cancelled
            AuthCodes.LINK_INVALID -> R.string.error_link_invalid
            ApiError.NETWORK -> R.string.error_network
            else -> R.string.error_generic
        }
    }
}
