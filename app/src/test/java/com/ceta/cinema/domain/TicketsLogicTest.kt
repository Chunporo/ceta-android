package com.ceta.cinema.domain

import com.ceta.cinema.core.QrMatrix
import com.ceta.cinema.domain.model.ShowFormat
import com.ceta.cinema.domain.model.Ticket
import com.ceta.cinema.domain.usecase.TicketGrouping
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TicketsLogicTest {
    private val now = Instant.parse("2026-11-18T07:00:00Z")

    private fun ticket(id: Long, booking: Long, seat: String, start: String, end: String? = null, used: Boolean = false) = Ticket(
        id, booking, "T$id", seat, 85_000, "CETA1.$id.x.y", used, 3, "Phim", null, "CETA Landmark", null, "R1", ShowFormat.TWO_D,
        Instant.parse(start), end?.let { Instant.parse(it) })

    @Test fun ticketsOfOneOrderBecomeOneCardWithSortedSeats() {
        val groups = TicketGrouping.group(listOf(ticket(2, 5, "F8", "2026-11-19T12:00:00Z"), ticket(1, 5, "F7", "2026-11-19T12:00:00Z"), ticket(3, 6, "A1", "2026-11-19T12:00:00Z")))
        assertEquals(2, groups.size)
        assertEquals(listOf("F7", "F8"), groups.first { it.bookingId == 5L }.seats)
        assertFalse(groups[0].allUsed)
    }

    @Test fun upcomingIsSoonestFirstAndWatchedIsMostRecentFirst() {
        val groups = TicketGrouping.group(listOf(
            ticket(1, 1, "A1", "2026-11-20T12:00:00Z", "2026-11-20T14:00:00Z"),
            ticket(2, 2, "A1", "2026-11-19T12:00:00Z", "2026-11-19T14:00:00Z"),
            ticket(3, 3, "A1", "2026-11-10T12:00:00Z", "2026-11-10T14:00:00Z"),
            ticket(4, 4, "A1", "2026-11-12T12:00:00Z", "2026-11-12T14:00:00Z")))
        val (upcoming, watched) = TicketGrouping.split(groups, now)
        assertEquals(listOf(2L, 1L), upcoming.map { it.bookingId })
        assertEquals(listOf(4L, 3L), watched.map { it.bookingId })
    }

    @Test fun aShowInProgressIsStillUpcomingUntilItEnds() {
        val g = TicketGrouping.group(listOf(ticket(1, 1, "A1", "2026-11-18T06:00:00Z", "2026-11-18T08:00:00Z")))
        assertFalse(TicketGrouping.isOver(g.single(), now))
        assertTrue(TicketGrouping.isOver(g.single(), Instant.parse("2026-11-18T08:00:00Z")))
    }

    @Test fun withoutAnEndTimeTheShowIsOverTwoHoursAfterItStarted() {
        val g = TicketGrouping.group(listOf(ticket(1, 1, "A1", "2026-11-18T05:30:00Z"))).single()
        assertFalse(TicketGrouping.isOver(g, Instant.parse("2026-11-18T07:29:00Z")))
        assertTrue(TicketGrouping.isOver(g, Instant.parse("2026-11-18T07:31:00Z")))
    }

    @Test fun anOrderIsUsedOnlyWhenEverySeatWasScanned() {
        val g = TicketGrouping.group(listOf(ticket(1, 1, "A1", "2026-11-19T12:00:00Z", used = true), ticket(2, 1, "A2", "2026-11-19T12:00:00Z", used = false))).single()
        assertFalse(g.allUsed)
        val all = TicketGrouping.group(listOf(ticket(1, 1, "A1", "2026-11-19T12:00:00Z", used = true))).single()
        assertTrue(all.allUsed)
    }

    // ---- QR

    @Test fun aQrCodeIsASquareOfModulesWithTheThreeFinderPatterns() {
        val m = QrMatrix.of("CETA1.9.1795000000.abcdef0123456789")
        assertTrue(m.size >= 21)
        assertTrue(m.all { it.size == m.size })
        val n = m.size
        // each finder pattern: a dark 7x7 ring with a dark 3x3 centre
        listOf(0 to 0, n - 7 to 0, 0 to n - 7).forEach { (x0, y0) ->
            for (i in 0..6) { assertTrue(m[y0][x0 + i]); assertTrue(m[y0 + 6][x0 + i]); assertTrue(m[y0 + i][x0]); assertTrue(m[y0 + i][x0 + 6]) }
            for (y in 2..4) for (x in 2..4) assertTrue(m[y0 + y][x0 + x])
            assertFalse("the ring around the centre is light", m[y0 + 1][x0 + 1])
        }
    }

    @Test fun theSameTextAlwaysGivesTheSameCodeAndDifferentTextDoesNot() {
        val a = QrMatrix.of("CETA1.1.x.y"); val b = QrMatrix.of("CETA1.1.x.y"); val c = QrMatrix.of("CETA1.2.x.y")
        assertTrue(a.indices.all { a[it].contentEquals(b[it]) })
        assertFalse(a.size == c.size && a.indices.all { a[it].contentEquals(c[it]) })
    }

    @Test fun nothingToEncodeIsRefused() {
        assertThrows(IllegalArgumentException::class.java) { QrMatrix.of("") }
    }
}
