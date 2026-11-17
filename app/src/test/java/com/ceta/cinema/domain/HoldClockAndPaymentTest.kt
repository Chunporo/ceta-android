package com.ceta.cinema.domain

import com.ceta.cinema.domain.model.Booking
import com.ceta.cinema.domain.model.BookingStatus
import com.ceta.cinema.domain.usecase.HoldClock
import com.ceta.cinema.domain.usecase.PaymentOutcome
import com.ceta.cinema.domain.usecase.PaymentPolling
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class HoldClockAndPaymentTest {
    private var now = Instant.parse("2026-11-17T10:00:00Z")
    private val clock = HoldClock { now }

    @Test fun beforeItStartsThereIsNothingToCountAndNothingExpired() {
        assertEquals(0, clock.remainingSeconds())
        assertFalse(clock.isExpired())
        assertFalse(clock.isWarning())
    }

    @Test fun itCountsDownFromTheServersDeadline() {
        clock.start(now.plusSeconds(600))
        assertEquals(600, clock.remainingSeconds())
        now = now.plusSeconds(90)
        assertEquals(510, clock.remainingSeconds())
        assertFalse(clock.isExpired())
    }

    @Test fun theLastTwoMinutesWarn() {
        clock.start(now.plusSeconds(600))
        now = now.plusSeconds(479)
        assertFalse(clock.isWarning())
        now = now.plusSeconds(1)       // 120 s left
        assertTrue(clock.isWarning())
        now = now.plusSeconds(119)     // 1 s left
        assertTrue(clock.isWarning())
        assertFalse(clock.isExpired())
    }

    @Test fun atTheDeadlineItIsExpiredAndNeverShowsNegativeTime() {
        clock.start(now.plusSeconds(60))
        now = now.plusSeconds(60)
        assertTrue(clock.isExpired())
        assertFalse("expired is not 'warning'", clock.isWarning())
        now = now.plusSeconds(500)
        assertEquals(0, clock.remainingSeconds())
    }

    @Test fun theServersSecondsRemainingCorrectsAPhoneClockThatDrifted() {
        clock.start(now.plusSeconds(600))
        // the customer sets the phone back an hour: the local math would give 4200 s
        now = now.minusSeconds(3600)
        assertEquals(4200, clock.remainingSeconds())
        clock.sync(540) // the next poll says the server has 540 s left
        assertEquals(540, clock.remainingSeconds())
    }

    @Test fun aNegativeServerValueMeansExpired() {
        clock.sync(-30)
        assertTrue(clock.isExpired())
    }

    private fun booking(status: BookingStatus, refund: Boolean = false) = Booking(
        1, status, 1000, null, 0, emptyList(), emptyList(), null, null, null, refund, 0, emptyList(), 0, null, 0, 0)

    @Test fun onlyAPaidBookingIsEverShownAsSuccess() {
        assertEquals(PaymentOutcome.PAID, PaymentPolling.outcome(booking(BookingStatus.PAID)))
        listOf(BookingStatus.PENDING, BookingStatus.UNKNOWN).forEach {
            assertEquals(PaymentOutcome.PROCESSING, PaymentPolling.outcome(booking(it)))
        }
    }

    @Test fun expiredCancelledAndRefundedHaveTheirOwnEnds() {
        assertEquals(PaymentOutcome.EXPIRED, PaymentPolling.outcome(booking(BookingStatus.EXPIRED)))
        assertEquals(PaymentOutcome.CANCELLED, PaymentPolling.outcome(booking(BookingStatus.CANCELLED)))
        assertEquals(PaymentOutcome.REFUND, PaymentPolling.outcome(booking(BookingStatus.REFUNDED)))
    }

    @Test fun aLatePaymentOnAnExpiredHoldIsARefundNotJustAnExpiry() {
        assertEquals(PaymentOutcome.REFUND, PaymentPolling.outcome(booking(BookingStatus.EXPIRED, refund = true)))
    }

    @Test fun waitingStatesAreNotTerminal() {
        assertFalse(PaymentOutcome.PROCESSING.terminal)
        assertFalse(PaymentOutcome.STILL_PENDING.terminal)
        assertTrue(PaymentOutcome.PAID.terminal && PaymentOutcome.REFUND.terminal && PaymentOutcome.EXPIRED.terminal && PaymentOutcome.CANCELLED.terminal)
    }

    @Test fun pollingBacksOffAndStopsNearAMinute() {
        assertEquals(listOf(2L, 3L, 5L, 8L), PaymentPolling.DELAYS_SECONDS.take(4))
        assertTrue(PaymentPolling.DELAYS_SECONDS.sum() in 55..65)
    }
}
