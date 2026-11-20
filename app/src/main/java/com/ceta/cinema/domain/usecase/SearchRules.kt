package com.ceta.cinema.domain.usecase

import com.ceta.cinema.domain.model.Cinema
import com.ceta.cinema.domain.model.Movie
import java.text.Normalizer

/** Lower-case and strip Vietnamese accents (and the stroke of đ), so "ngu mieu" finds "Ngự Miêu" and "dune" finds "Dune". */
fun String.foldForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").replace('đ', 'd').replace('Đ', 'D').lowercase().trim()

enum class SearchFilter { ALL, NOW, SOON, CINEMA }

/** A movie that matched; [person] names the director or actor when the match came from them rather than from the title or genre. */
data class MovieHit(val movie: Movie, val person: String?)

data class SearchResults(val movies: List<MovieHit>, val cinemas: List<Cinema>) {
    val now: List<MovieHit> get() = movies.filter { it.movie.isNowShowing }
    val soon: List<MovieHit> get() = movies.filter { !it.movie.isNowShowing }

    fun count(filter: SearchFilter): Int = when (filter) {
        SearchFilter.ALL -> movies.size + cinemas.size
        SearchFilter.NOW -> now.size
        SearchFilter.SOON -> soon.size
        SearchFilter.CINEMA -> cinemas.size
    }
}

/**
 * Local search over what the catalog already holds (design item 16): title, genre, director, cast, and a cinema's name and address,
 * ignoring case and accents. Every word typed must be found somewhere in the item.
 * TODO(backend): GET /search?q= for large catalogs; today the whole catalog is small enough to filter on the phone.
 */
object SearchEngine {
    fun search(query: String, movies: List<Movie>, cinemas: List<Cinema>): SearchResults {
        val words = query.foldForSearch().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return SearchResults(emptyList(), emptyList())
        val hits = movies.mapNotNull { m ->
            val title = m.title.foldForSearch()
            val people = (listOfNotNull(m.director) + m.cast).map { it to it.foldForSearch() }
            val haystack = listOf(title, m.genres.joinToString(" ").foldForSearch()) + people.map { it.second }
            if (!words.all { w -> haystack.any { w in it } }) return@mapNotNull null
            val titleOrGenre = words.all { w -> w in title || w in m.genres.joinToString(" ").foldForSearch() }
            val person = if (titleOrGenre) null else people.firstOrNull { (_, f) -> words.any { it in f } }?.first
            MovieHit(m, person)
        }.sortedWith(compareByDescending<MovieHit> { it.movie.isNowShowing }.thenBy { it.movie.title.foldForSearch() })
        val places = cinemas.filter { c ->
            val hay = (c.name + " " + c.address).foldForSearch()
            words.all { it in hay }
        }.sortedBy { it.name }
        return SearchResults(hits, places)
    }
}
