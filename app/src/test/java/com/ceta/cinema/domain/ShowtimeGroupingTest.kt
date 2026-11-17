package com.ceta.cinema.domain

import com.ceta.cinema.domain.model.LanguageMode
import com.ceta.cinema.domain.model.ShowFormat
import com.ceta.cinema.domain.model.Showtime
import com.ceta.cinema.domain.usecase.DayPart
import com.ceta.cinema.domain.usecase.ShowtimeGrouping
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ShowtimeGroupingTest {
    private val now = Instant.parse("2026-11-18T07:00:00Z") // 14:00 in Vietnam

    private fun show(id: Long, utc: String, cinema: String? = "CETA Landmark", cinemaId: Long? = 1, format: ShowFormat = ShowFormat.TWO_D, cancelled: Boolean = false) =
        Showtime(id, "m", "Phim", null, cinemaId, cinema, "R1", Instant.parse(utc), null, 85_000, format, LanguageMode.SUB, cancelled)

    @Test fun formatFilterKeepsOnlyThatFormatInTimeOrder() {
        val list = listOf(show(2, "2026-11-18T13:00:00Z", format = ShowFormat.IMAX), show(1, "2026-11-18T12:00:00Z"), show(3, "2026-11-18T11:00:00Z", format = ShowFormat.IMAX))
        assertEquals(listOf(3L, 2L), ShowtimeGrouping.filter(list, ShowFormat.IMAX).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), ShowtimeGrouping.filter(list, null).map { it.id })
    }

    @Test fun pastAndCancelledShowsAreVisibleButNotTappable() {
        val past = show(1, "2026-11-18T05:00:00Z")      // 12:00, already over at 14:00
        val cancelled = show(2, "2026-11-18T12:00:00Z", cancelled = true)
        val ok = show(3, "2026-11-18T12:30:00Z")
        val chips = ShowtimeGrouping.byCinema(listOf(past, cancelled, ok), now).single().chips
        assertEquals(listOf(false, false, true), chips.map { it.enabled })
    }

    @Test fun showsAreGroupedByCinemaAndTimedWithinEachOne() {
        val groups = ShowtimeGrouping.byCinema(listOf(
            show(1, "2026-11-18T13:00:00Z", "CETA Thảo Điền", 3), show(2, "2026-11-18T12:00:00Z", "CETA Landmark", 1), show(3, "2026-11-18T11:00:00Z", "CETA Landmark", 1)), now)
        assertEquals(listOf("CETA Landmark", "CETA Thảo Điền"), groups.map { it.cinemaName })
        assertEquals(listOf(3L, 2L), groups[0].chips.map { it.showtime.id })
    }

    @Test fun dayPartsFollowTheCinemasClockNotUtc() {
        assertEquals(DayPart.MORNING, ShowtimeGrouping.dayPart(Instant.parse("2026-11-18T03:00:00Z")))     // 10:00
        assertEquals(DayPart.AFTERNOON, ShowtimeGrouping.dayPart(Instant.parse("2026-11-18T05:00:00Z")))   // 12:00
        assertEquals(DayPart.AFTERNOON, ShowtimeGrouping.dayPart(Instant.parse("2026-11-18T10:59:00Z")))   // 17:59
        assertEquals(DayPart.EVENING, ShowtimeGrouping.dayPart(Instant.parse("2026-11-18T11:00:00Z")))     // 18:00
        assertEquals("a late show after midnight is the next morning's part", DayPart.MORNING, ShowtimeGrouping.dayPart(Instant.parse("2026-11-18T18:00:00Z"))) // 01:00 next day
    }

    @Test fun byDayPartKeepsEachPartSorted() {
        val parts = ShowtimeGrouping.byDayPart(listOf(show(1, "2026-11-18T12:00:00Z"), show(2, "2026-11-18T03:00:00Z"), show(3, "2026-11-18T09:00:00Z")), now)
        assertEquals(listOf(2L), parts[DayPart.MORNING]!!.map { it.showtime.id })
        assertEquals(listOf(3L), parts[DayPart.AFTERNOON]!!.map { it.showtime.id })
        assertEquals(listOf(1L), parts[DayPart.EVENING]!!.map { it.showtime.id })
    }

    @Test fun theWeekStartsToday() {
        val week = ShowtimeGrouping.week(LocalDate.of(2026, 11, 28))
        assertEquals(7, week.size)
        assertEquals(LocalDate.of(2026, 11, 28), week.first())
        assertEquals(LocalDate.of(2026, 12, 4), week.last())
    }
}
