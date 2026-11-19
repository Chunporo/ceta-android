package com.ceta.cinema.domain.usecase

import com.ceta.cinema.core.CinemaZone
import com.ceta.cinema.domain.model.Movie
import com.ceta.cinema.domain.model.MovieStatus
import com.ceta.cinema.domain.model.ShowFormat
import com.ceta.cinema.domain.model.Showtime
import java.time.Instant

/** "Suất gần nhất" under the home carousel: today's next times of one movie at one cinema. */
data class NextShowtimes(val cinemaName: String?, val chips: List<Showtime>) {
    val none: Boolean get() = chips.isEmpty()
}

object NextShowtimesPicker {
    const val LIMIT = 4

    /**
     * Today's shows that can still be booked (not cancelled, not sold out, starting after [now]), at one cinema, earliest first.
     * The app has no "chosen cinema" yet, so the cinema is [preferredCinemaId] when it has such a show, otherwise the one that
     * starts soonest (ties go to the lower id), so the customer sees a time that is really coming up.
     */
    fun pick(shows: List<Showtime>, now: Instant, preferredCinemaId: Long? = null, limit: Int = LIMIT): NextShowtimes {
        val today = now.atZone(CinemaZone).toLocalDate()
        val open = shows.filter { !it.cancelled && !it.full && it.start.isAfter(now) && it.start.atZone(CinemaZone).toLocalDate() == today }
        if (open.isEmpty()) return NextShowtimes(null, emptyList())
        val byCinema = open.groupBy { it.cinemaId }
        val cinema = preferredCinemaId?.takeIf { byCinema.containsKey(it) }
            ?: byCinema.entries.minWith(compareBy({ e -> e.value.minOf { it.start } }, { it.key ?: Long.MAX_VALUE })).key
        val chosen = byCinema.getValue(cinema).sortedBy { it.start }.take(limit)
        return NextShowtimes(chosen.first().cinemaName, chosen)
    }
}

/** The label in the carousel's top-left corner. The API has no field for it yet, so it is read from what the movie already says. */
enum class FeaturedTag {
    IMAX, HOT, EARLY;

    companion object {
        private const val HOT_SCORE = 8.5
        private const val HOT_VOTES = 20L

        fun of(movie: Movie): FeaturedTag? = when {
            movie.status == MovieStatus.EARLY_ACCESS -> EARLY
            ShowFormat.IMAX in movie.formats -> IMAX
            (movie.ratingAverage ?: 0.0) >= HOT_SCORE && movie.ratingCount >= HOT_VOTES -> HOT
            else -> null
        }
    }
}
