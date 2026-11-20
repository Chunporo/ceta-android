package com.ceta.cinema.domain

import com.ceta.cinema.domain.usecase.CancelPolicy
import com.ceta.cinema.domain.usecase.ShowWhen
import com.ceta.cinema.domain.usecase.TicketGrouping
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class CancelPolicyTest {
    private val show = Instant.parse("2026-11-20T12:00:00Z")

    @Test fun aTicketCanBeCancelledUntilTwoHoursBeforeTheShow() {
        assertTrue(CancelPolicy.canCancel(show, show.minusSeconds(2 * 3600), anyTicketUsed = false))
        assertFalse(CancelPolicy.canCancel(show, show.minusSeconds(2 * 3600 - 1), anyTicketUsed = false))
        assertFalse("a ticket already scanned at the door stays", CancelPolicy.canCancel(show, show.minusSeconds(5 * 3600), anyTicketUsed = true))
    }

    @Test fun theRefundIsTheWholeOrder() = assertEquals(170_000L, CancelPolicy.refundFor(170_000))

    @Test fun theChipCountsCinemaCalendarDays() {
        val now = Instant.parse("2026-11-20T03:00:00Z") // 10:00 in Ho Chi Minh City
        assertEquals(ShowWhen.Today, TicketGrouping.whenIs(Instant.parse("2026-11-20T14:00:00Z"), now))
        assertEquals(ShowWhen.Tomorrow, TicketGrouping.whenIs(Instant.parse("2026-11-21T01:00:00Z"), now))
        assertEquals(ShowWhen.InDays(4), TicketGrouping.whenIs(Instant.parse("2026-11-24T03:00:00Z"), now))
    }
}
