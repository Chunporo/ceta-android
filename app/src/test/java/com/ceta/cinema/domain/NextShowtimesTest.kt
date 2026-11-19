package com.ceta.cinema.domain

import com.ceta.cinema.domain.model.*
import com.ceta.cinema.domain.usecase.FeaturedTag
import com.ceta.cinema.domain.usecase.NextShowtimesPicker
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class NextShowtimesTest {
    private val now = Instant.parse("2026-11-18T07:00:00Z") // 14:00 in Vietnam

    private fun show(id: Long, utc: String, cinemaId: Long = 1, cancelled: Boolean = false, free: Int? = null, format: ShowFormat = ShowFormat.TWO_D) =
        Showtime(id, "m", "Phim", null, cinemaId, "Rạp $cinemaId", "R1", Instant.parse(utc), null, 85_000, format, LanguageMode.SUB, cancelled,
            availableSeats = free, totalSeats = free?.let { 100 })

    @Test fun onlyTodaysBookableShowsAfterNowAtOneCinemaEarliestFirstMaxFour() {
        val shows = listOf(
            show(1, "2026-11-18T05:00:00Z"),                      // 12:00, already started
            show(2, "2026-11-18T08:00:00Z", cancelled = true),    // cancelled
            show(3, "2026-11-18T08:30:00Z", free = 0),            // sold out
            show(4, "2026-11-19T02:00:00Z"),                      // tomorrow
            show(10, "2026-11-18T14:00:00Z"), show(11, "2026-11-18T09:00:00Z"), show(12, "2026-11-18T10:00:00Z"),
            show(13, "2026-11-18T11:00:00Z"), show(14, "2026-11-18T12:00:00Z"),
        )
        val next = NextShowtimesPicker.pick(shows, now)
        assertEquals(listOf(11L, 12L, 13L, 14L), next.chips.map { it.id })
        assertEquals("Rạp 1", next.cinemaName)
    }

    @Test fun theCinemaWithTheSoonestShowWinsUnlessOneIsPreferred() {
        val shows = listOf(show(1, "2026-11-18T10:00:00Z", cinemaId = 1), show(2, "2026-11-18T08:00:00Z", cinemaId = 2), show(3, "2026-11-18T09:00:00Z", cinemaId = 2))
        assertEquals(listOf(2L, 3L), NextShowtimesPicker.pick(shows, now).chips.map { it.id })
        assertEquals(listOf(1L), NextShowtimesPicker.pick(shows, now, preferredCinemaId = 1).chips.map { it.id })
        // A preferred cinema with nothing left today is ignored rather than leaving the card empty.
        assertEquals(listOf(2L, 3L), NextShowtimesPicker.pick(shows, now, preferredCinemaId = 9).chips.map { it.id })
    }

    @Test fun nothingLeftTodayGivesNoChips() {
        assertTrue(NextShowtimesPicker.pick(listOf(show(1, "2026-11-18T05:00:00Z"), show(2, "2026-11-19T02:00:00Z")), now).none)
        assertTrue(NextShowtimesPicker.pick(emptyList(), now).none)
    }

    @Test fun lateEveningShowStaysOnTodayByTheCinemasClockNotUtc() {
        // 23:30 in Vietnam is 16:30 UTC the same day; 00:30 the next day in Vietnam is still 17:30 UTC today and must not count.
        val shows = listOf(show(1, "2026-11-18T16:30:00Z"), show(2, "2026-11-18T17:30:00Z"))
        assertEquals(listOf(1L), NextShowtimesPicker.pick(shows, now).chips.map { it.id })
    }

    private fun movie(status: MovieStatus = MovieStatus.NOW_SHOWING, formats: Set<ShowFormat> = emptySet(), score: Double? = null, votes: Long = 0) =
        Movie("m", "Phim", null, "T13", status, 100, null, null, null, null, null, score, votes, null, emptyList(), emptyList(), formats)

    @Test fun tagComesFromWhatTheMovieAlreadySays() {
        assertEquals(FeaturedTag.EARLY, FeaturedTag.of(movie(status = MovieStatus.EARLY_ACCESS, formats = setOf(ShowFormat.IMAX))))
        assertEquals(FeaturedTag.IMAX, FeaturedTag.of(movie(formats = setOf(ShowFormat.IMAX), score = 9.0, votes = 100)))
        assertEquals(FeaturedTag.HOT, FeaturedTag.of(movie(score = 8.6, votes = 50)))
        assertNull(FeaturedTag.of(movie(score = 8.6, votes = 3)))   // a high score from a few votes is not "hot"
        assertNull(FeaturedTag.of(movie()))
    }
}
