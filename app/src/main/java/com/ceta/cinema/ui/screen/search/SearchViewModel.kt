package com.ceta.cinema.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.CinemaZone
import com.ceta.cinema.data.local.prefs.SettingsStore
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.data.repository.TrendingItem
import com.ceta.cinema.data.repository.TrendingRepository
import com.ceta.cinema.domain.model.Cinema
import com.ceta.cinema.domain.model.Genre
import com.ceta.cinema.domain.model.Movie
import com.ceta.cinema.domain.usecase.NextShowtimes
import com.ceta.cinema.domain.usecase.NextShowtimesPicker
import com.ceta.cinema.domain.usecase.SearchEngine
import com.ceta.cinema.domain.usecase.SearchFilter
import com.ceta.cinema.domain.usecase.SearchResults
import com.ceta.cinema.domain.usecase.foldForSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** One genre tile: its name, how many movies it holds and the posters of two of them. */
data class GenreCard(val genre: Genre, val count: Int, val posters: List<String?>)

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val recents: List<String> = emptyList(),
    val genres: List<GenreCard> = emptyList(),
    val trending: List<TrendingItem> = emptyList(),
    /** Null until something has been typed. */
    val results: SearchResults? = null,
    /** Today's next showtimes for the movies in the results that are in cinemas. */
    val shows: Map<String, NextShowtimes> = emptyMap(),
    val loading: Boolean = true,
    val error: ApiError? = null,
) {
    val idle: Boolean get() = query.isBlank()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val settings: SettingsStore,
    private val trending: TrendingRepository,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state
    private var movies: List<Movie> = emptyList()
    private var cinemas: List<Cinema> = emptyList()
    private var job: Job? = null

    init {
        viewModelScope.launch { settings.settings.collect { s -> _state.update { it.copy(recents = s.recentSearches) } } }
        viewModelScope.launch { loadCatalog() }
    }

    /** Everything searchable is fetched once (the catalog is cached for hours); typing then filters it on the phone. */
    private suspend fun loadCatalog() {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = catalog.movies()) {
            is ApiResult.Err -> { _state.update { it.copy(loading = false, error = result.error) }; return }
            is ApiResult.Ok -> movies = result.data
        }
        cinemas = (catalog.cinemas() as? ApiResult.Ok)?.data.orEmpty()
        val genres = (catalog.genres() as? ApiResult.Ok)?.data.orEmpty()
        val cards = genres.map { g ->
            val inGenre = movies.filter { m -> m.genres.any { it.foldForSearch() == g.name.foldForSearch() } }
            GenreCard(g, inGenre.size, inGenre.take(2).map { it.posterUrl })
        }.filter { it.count > 0 } // a genre with no film is not worth a tile
        val top = (trending.top() as? ApiResult.Ok)?.data.orEmpty()
        _state.update { it.copy(loading = false, genres = cards, trending = top) }
        // A query typed while the catalog was still loading is answered now.
        if (_state.value.query.isNotBlank()) run(_state.value.query)
    }

    fun retry() { viewModelScope.launch { loadCatalog() } }

    /** Typing searches after a short pause, so a fast typist does not redo the work on every key. */
    fun onQuery(value: String) {
        _state.update { it.copy(query = value) }
        job?.cancel()
        if (value.isBlank()) {
            _state.update { it.copy(results = null, shows = emptyMap(), filter = SearchFilter.ALL) }
            return
        }
        job = viewModelScope.launch { delay(DEBOUNCE_MS); run(value) }
    }

    fun setFilter(filter: SearchFilter) = _state.update { it.copy(filter = filter) }

    /** A genre tile searches by the genre's name straight away. */
    fun pickGenre(genre: Genre) {
        job?.cancel()
        _state.update { it.copy(query = genre.name) }
        viewModelScope.launch { run(genre.name) }
    }

    fun pickRecent(query: String) {
        job?.cancel()
        _state.update { it.copy(query = query) }
        viewModelScope.launch { run(query) }
    }

    /** Called when the keyboard's search key is pressed or a result is opened: only then is the query worth remembering. */
    fun remember() {
        val q = _state.value.query
        if (q.isNotBlank()) viewModelScope.launch { settings.addRecentSearch(q) }
    }

    fun clearRecents() { viewModelScope.launch { settings.clearRecentSearches() } }

    fun removeRecent(query: String) { viewModelScope.launch { settings.removeRecentSearch(query) } }

    private suspend fun run(query: String) {
        val found = SearchEngine.search(query, movies, cinemas)
        _state.update { it.copy(results = found, filter = if (found.count(it.filter) == 0) SearchFilter.ALL else it.filter) }
        loadShows(found)
    }

    /** The movies in cinemas that matched get their next showtimes of today (the first few only, to keep requests down). */
    private suspend fun loadShows(found: SearchResults) {
        val today = LocalDate.now(clock.withZone(CinemaZone))
        val now = clock.instant()
        for (hit in found.now.take(MAX_WITH_SHOWS)) {
            val id = hit.movie.id
            if (_state.value.shows.containsKey(id)) continue
            val list = (catalog.showtimesForMovie(id, today) as? ApiResult.Ok)?.data ?: continue
            val next = NextShowtimesPicker.pick(list, now, limit = SHOWS_LIMIT)
            _state.update { it.copy(shows = it.shows + (id to next)) }
        }
    }

    companion object {
        const val DEBOUNCE_MS = 250L
        const val MAX_WITH_SHOWS = 8
        const val SHOWS_LIMIT = 3
    }
}
