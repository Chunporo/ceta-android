package com.ceta.cinema.core

import com.ceta.cinema.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ErrorMapperTest {
    private fun msg(code: String, status: Int = 400) = ErrorMapper.messageFor(ApiError(code, status))

    @Test fun every_backend_code_has_its_own_text() {
        val codes = listOf(
            "SEAT_CONFLICT", "BOOKING_ALREADY_PENDING", "HOLD_EXPIRED", "MAX_SEATS_EXCEEDED", "SHOWTIME_CLOSED",
            "SHOWTIME_CANCELLED", "INVALID_COUPLE_SEAT", "BOOKING_NOT_CANCELLABLE", "TOO_MANY_ATTEMPTS",
            "TICKET_ALREADY_USED", "HAS_ACTIVE_TICKETS", "ACCOUNT_LOCKED", "EMAIL_NOT_VERIFIED", "VOUCHER_INVALID",
            "VOUCHER_NOT_ELIGIBLE", "COMBO_UNAVAILABLE", "POINTS_NOT_APPLICABLE", "INSUFFICIENT_POINTS", "PAYMENT_ALREADY_STARTED",
            "BOOKING_NOT_EDITABLE", "REVIEW_NOT_ALLOWED", "ALREADY_ON_SALE",
        )
        val texts = codes.map(::msg)
        assertEquals("each code maps to a different string", codes.size, texts.toSet().size)
        texts.forEach { assertNotEquals(R.string.error_generic, it) }
    }

    @Test fun http_401_means_session_expired_whatever_the_code() {
        assertEquals(R.string.error_session_expired, msg("ANYTHING", 401))
        assertEquals(R.string.error_session_expired, msg(ApiError.UNAUTHORIZED, 0))
    }

    @Test fun network_and_unknown_codes() {
        assertEquals(R.string.error_network, ErrorMapper.messageFor(ApiError(ApiError.NETWORK, 0)))
        assertEquals(R.string.error_generic, msg("SOMETHING_NEW", 500))
    }

    @Test fun sign_in_codes_have_their_own_text() {
        val codes = listOf(AuthCodes.INVALID_CREDENTIALS, AuthCodes.EMAIL_IN_USE, AuthCodes.WEAK_PASSWORD, AuthCodes.CANCELLED, AuthCodes.LINK_INVALID)
        assertEquals(codes.size, codes.map { msg(it) }.toSet().size)
        codes.forEach { assertNotEquals(R.string.error_generic, msg(it)) }
    }
}
