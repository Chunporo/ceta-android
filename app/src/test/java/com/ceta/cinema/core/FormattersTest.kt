package com.ceta.cinema.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class FormattersTest {
    @Test fun votesLikeTheDesign() {
        assertEquals("12,4K", Formatters.votes(12_400))
        assertEquals("15K", Formatters.votes(15_000))
        assertEquals("999", Formatters.votes(999))
        assertEquals("1K", Formatters.votes(1_000))
        assertEquals("0", Formatters.votes(0))
    }

    @Test fun scoreHasOneDecimalOrADash() {
        assertEquals("8.6", Formatters.score(8.6))
        assertEquals("9.0", Formatters.score(9.0))
        assertEquals("—", Formatters.score(null))
    }

    @Test fun timesAreCinemaLocalNotUtc() {
        // 12:30 UTC is 19:30 in Vietnam
        assertEquals("19:30", Formatters.time(Instant.parse("2026-11-18T12:30:00Z")))
    }

    @Test fun theCinemaDayCanDifferFromTheUtcDay() {
        // 20:00 UTC on the 17th is already the 18th in Vietnam
        assertEquals(LocalDate.of(2026, 11, 18), Formatters.localDate(Instant.parse("2026-11-17T20:00:00Z")))
    }

    @Test fun dayChipsInBothLanguages() {
        assertEquals("T3, 17/11", Formatters.dayChip(LocalDate.of(2026, 11, 17), english = false))
        assertEquals("CN, 22/11", Formatters.dayChip(LocalDate.of(2026, 11, 22), english = false))
        assertEquals("Tue, 17 Nov", Formatters.dayChip(LocalDate.of(2026, 11, 17), english = true))
    }

    @Test fun countdownNeverShowsNegativeTime() {
        assertEquals("2:00", Formatters.mmss(120))
        assertEquals("0:09", Formatters.mmss(9))
        assertEquals("0:00", Formatters.mmss(-5))
        assertEquals("09:56", Formatters.clock(596))
        assertEquals("10:00", Formatters.clock(600))
        assertEquals("00:00", Formatters.clock(-3))
    }

    @Test fun dates() = assertEquals("07/11/2026", Formatters.date(LocalDate.of(2026, 11, 7)))

    @Test fun greetingUsesTheGivenNameWhicheverEndOfTheNameItIsOn() {
        assertEquals("Trung", Formatters.greetingName("Huynh Quoc Trung", vietnamese = true))
        assertEquals("Ada", Formatters.greetingName("Ada Lovelace", vietnamese = false))
        assertEquals("Minh", Formatters.greetingName("  Minh ", vietnamese = true))
        assertEquals("", Formatters.greetingName("   ", vietnamese = true))
    }
}
