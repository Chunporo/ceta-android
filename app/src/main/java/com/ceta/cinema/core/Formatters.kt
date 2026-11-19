package com.ceta.cinema.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The cinema's own time zone: show times are read as local time, never the phone's. */
val CinemaZone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

object Formatters {
    /** 12400 -> "12,4K" like the design; small counts stay whole. */
    fun votes(count: Long): String = when {
        count < 1000 -> count.toString()
        else -> "%.1f".format(Locale.ROOT, count / 1000.0).replace('.', ',').removeSuffix(",0") + "K"
    }

    fun score(value: Double?): String = if (value == null) "—" else "%.1f".format(Locale.ROOT, value)

    fun date(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    fun time(instant: Instant): String = instant.atZone(CinemaZone).format(DateTimeFormatter.ofPattern("HH:mm"))

    fun localDate(instant: Instant): LocalDate = instant.atZone(CinemaZone).toLocalDate()

    /** "T3, 17/11" (vi) or "Tue, 17 Nov" (en), like the design's date chips. */
    fun dayChip(date: LocalDate, english: Boolean): String {
        val loc = if (english) Locale.ENGLISH else Locale.forLanguageTag("vi")
        return if (english) date.format(DateTimeFormatter.ofPattern("EEE, d MMM", loc))
        else "${viWeekday(date)}, ${date.format(DateTimeFormatter.ofPattern("dd/MM"))}"
    }

    /** The short weekday on a date tile: T2..CN in Vietnamese, Mon..Sun in English. */
    fun weekdayShort(date: LocalDate, english: Boolean): String =
        if (english) date.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)) else viWeekday(date)

    private fun viWeekday(date: LocalDate) = when (date.dayOfWeek.value) {
        1 -> "T2"; 2 -> "T3"; 3 -> "T4"; 4 -> "T5"; 5 -> "T6"; 6 -> "T7"; else -> "CN"
    }

    /**
     * The name for "Xin chào, Minh": the given name. A Vietnamese name ends with it ("Huỳnh Quốc Trung" -> "Trung"), an English one
     * starts with it ("Ada Lovelace" -> "Ada"); a single word stays as it is.
     */
    fun greetingName(fullName: String, vietnamese: Boolean): String {
        val words = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            words.isEmpty() -> ""
            vietnamese -> words.last()
            else -> words.first()
        }
    }

    /** The seat-hold chip: "09:56" (always two digits each, never negative). */
    fun clock(totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0)
        return "%02d:%02d".format(s / 60, s % 60)
    }

    /** Whole minutes as the design writes them: "128 phút" / "128 min". */
    fun mmss(totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0)
        return "%d:%02d".format(s / 60, s % 60)
    }
}
