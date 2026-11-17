package com.ceta.cinema.domain.usecase

import com.ceta.cinema.core.CinemaZone
import com.ceta.cinema.domain.model.ShowFormat
import com.ceta.cinema.domain.model.Showtime
import java.time.Instant
import java.time.LocalDate

enum class DayPart { MORNING, AFTERNOON, EVENING }

/** One time chip: past and cancelled shows stay visible but cannot be tapped. */
data class ShowChip(val showtime: Showtime, val enabled: Boolean)

data class CinemaShows(val cinemaId: Long?, val cinemaName: String, val chips: List<ShowChip>)

/** One movie's times at a cinema, for the Cinemas tab. */
data class MovieShows(val movieId: String?, val movieTitle: String, val posterUrl: String?, val chips: List<ShowChip>)

object ShowtimeGrouping {
    fun byMovie(showtimes: List<Showtime>, now: Instant): List<MovieShows> =
        showtimes.groupBy { it.movieId to (it.movieTitle ?: "") }.map { (key, list) ->
            MovieShows(key.first, key.second, list.first().posterUrl, list.sortedBy { it.start }.map { chip(it, now) })
        }.sortedBy { it.movieTitle }

    fun chip(showtime: Showtime, now: Instant) = ShowChip(showtime, enabled = !showtime.cancelled && showtime.start.isAfter(now))

    fun filter(showtimes: List<Showtime>, format: ShowFormat?): List<Showtime> =
        showtimes.filter { format == null || it.format == format }.sortedBy { it.start }

    fun byCinema(showtimes: List<Showtime>, now: Instant): List<CinemaShows> =
        showtimes.groupBy { it.cinemaId to (it.cinemaName ?: "") }.map { (key, list) ->
            CinemaShows(key.first, key.second, list.sortedBy { it.start }.map { chip(it, now) })
        }.sortedBy { it.cinemaName }

    fun dayPart(start: Instant): DayPart = when (start.atZone(CinemaZone).hour) {
        in 0..11 -> DayPart.MORNING
        in 12..17 -> DayPart.AFTERNOON
        else -> DayPart.EVENING
    }

    fun byDayPart(showtimes: List<Showtime>, now: Instant): Map<DayPart, List<ShowChip>> =
        showtimes.sortedBy { it.start }.groupBy { dayPart(it.start) }.mapValues { (_, v) -> v.map { chip(it, now) } }

    /** The week the date chips offer, starting today in the cinema's own time zone. */
    fun week(today: LocalDate, days: Int = 7): List<LocalDate> = (0 until days).map { today.plusDays(it.toLong()) }
}
