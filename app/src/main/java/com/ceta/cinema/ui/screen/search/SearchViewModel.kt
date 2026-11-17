package com.ceta.cinema.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.data.local.prefs.SettingsStore
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.domain.model.Genre
import com.ceta.cinema.domain.model.Movie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val genre: Genre? = null,
    val recents: List<String> = emptyList(),
    val genres: List<Genre> = emptyList(),
    /** Null until something has been searched; empty means "nothing matched". */
    val results: List<Movie>? = null,
    val loading: Boolean = false,
    val error: ApiError? = null,
) {
    val idle: Boolean get() = query.isBlank() && genre == null
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val settings: SettingsStore,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state
    private var job: Job? = null

    init {
        viewModelScope.launch { settings.settings.collect { s -> _state.update { it.copy(recents = s.recentSearches) } } }
        viewModelScope.launch { (catalog.genres() as? ApiResult.Ok)?.let { r -> _state.update { it.copy(genres = r.data) } } }
    }

    /** Typing searches after a short pause, so each keystroke does not cost a request. */
    fun onQuery(value: String) {
        _state.update { it.copy(query = value, genre = null, error = null) }
        job?.cancel()
        if (value.isBlank()) {
            _state.update { it.copy(results = null, loading = false) }
            return
        }
        job = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            search(value, genre = null)
        }
    }

    fun pickGenre(genre: Genre) {
        job?.cancel()
        _state.update { it.copy(query = "", genre = genre, error = null) }
        job = viewModelScope.launch { search(null, genre.slug) }
    }

    /** Called when the keyboard's search key is pressed or a result is opened: only then is the query worth remembering. */
    fun remember() {
        val q = _state.value.query
        if (q.isNotBlank()) viewModelScope.launch { settings.addRecentSearch(q) }
    }

    fun clearRecents() { viewModelScope.launch { settings.clearRecentSearches() } }

    fun pickRecent(query: String) = onQuery(query)

    private suspend fun search(q: String?, genre: String?) {
        _state.update { it.copy(loading = true) }
        when (val result = catalog.movies(q = q, genre = genre)) {
            is ApiResult.Ok -> _state.update { it.copy(loading = false, results = result.data, error = null) }
            is ApiResult.Err -> _state.update { it.copy(loading = false, error = result.error) }
        }
    }

    companion object { const val DEBOUNCE_MS = 300L }
}
