package com.ceta.cinema.domain

import com.ceta.cinema.data.local.prefs.withRecent
import com.ceta.cinema.domain.model.Cinema
import com.ceta.cinema.domain.model.Movie
import com.ceta.cinema.domain.model.MovieStatus
import com.ceta.cinema.domain.usecase.SearchEngine
import com.ceta.cinema.domain.usecase.SearchFilter
import com.ceta.cinema.domain.usecase.foldForSearch
import org.junit.Assert.*
import org.junit.Test

class SearchRulesTest {
    private fun movie(id: String, title: String, status: MovieStatus = MovieStatus.NOW_SHOWING, director: String? = null, cast: List<String> = emptyList(), genres: List<String> = emptyList()) =
        Movie(id, title, null, null, status, null, null, null, director, null, null, null, 0, null, cast, genres, emptySet())

    private val movies = listOf(
        movie("1", "Mùa Hè Rực Lửa", cast = listOf("Thu Trang"), genres = listOf("Hành động")),
        movie("2", "Đại Chiến Vũ Trụ", MovieStatus.COMING_SOON, director = "Lê Văn Đạo", genres = listOf("Khoa học viễn tưởng")),
    )
    private val cinemas = listOf(Cinema(1, "CETA Landmark", "HCM", "208 Nguyễn Hữu Cảnh", null, null, true))

    @Test fun foldingDropsAccentsCaseAndTheStrokeOfD() {
        assertEquals("dai chien vu tru", "Đại Chiến Vũ Trụ".foldForSearch())
        assertEquals("ngu mieu pha an", "  Ngự Miêu Phá Án ".foldForSearch())
    }

    @Test fun everyWordTypedMustBeFound() {
        assertEquals(listOf("1"), SearchEngine.search("mua ruc", movies, cinemas).movies.map { it.movie.id })
        assertTrue(SearchEngine.search("mua zzz", movies, cinemas).movies.isEmpty())
    }

    @Test fun titleGenreDirectorAndCastAllMatch() {
        assertEquals(listOf("1"), SearchEngine.search("hanh dong", movies, cinemas).movies.map { it.movie.id })
        assertEquals("Lê Văn Đạo", SearchEngine.search("le van dao", movies, cinemas).movies.single().person)
        assertEquals("Thu Trang", SearchEngine.search("thu trang", movies, cinemas).movies.single().person)
        assertNull("a title match names no person", SearchEngine.search("mua he", movies, cinemas).movies.single().person)
    }

    @Test fun cinemasMatchByNameOrAddressAndCountsFollowTheFilters() {
        val r = SearchEngine.search("nguyen huu canh", movies, cinemas)
        assertEquals(listOf("CETA Landmark"), r.cinemas.map { it.name })
        val all = SearchEngine.search("a", movies, cinemas)
        assertEquals(all.movies.size + all.cinemas.size, all.count(SearchFilter.ALL))
        assertEquals(1, SearchEngine.search("dai chien", movies, cinemas).count(SearchFilter.SOON))
        assertEquals(0, SearchEngine.search("dai chien", movies, cinemas).count(SearchFilter.NOW))
    }

    @Test fun nowShowingComesBeforeComingSoon() {
        val r = SearchEngine.search("a", movies, emptyList())
        assertEquals(listOf("1", "2"), r.movies.map { it.movie.id })
    }

    @Test fun recentSearchesIgnoreAccentsWhenDeduplicating() {
        assertEquals(listOf("Mùa hè", "dune"), listOf("dune", "mua he").withRecent("Mùa hè"))
    }
}
