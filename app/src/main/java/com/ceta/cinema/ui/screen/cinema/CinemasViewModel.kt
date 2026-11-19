package com.ceta.cinema.ui.screen.cinema

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.CinemaZone
import com.ceta.cinema.core.OfflineFlag
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.domain.model.Cinema
import com.ceta.cinema.domain.model.Showtime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class CinemasUi(
    val cinemas: List<Cinema>,
    val city: String,
    /** The cinema whose card is open, and each opened cinema's showings for today. */
    val open: Long? = null,
    val shows: Map<Long, UiState<List<Showtime>>> = emptyMap(),
) {
    val cities: List<String> get() = cinemas.map { it.city }.distinct().sorted()
    val visible: List<Cinema> get() = cinemas.filter { it.city == city }.sortedBy { it.name }
}

@HiltViewModel
class CinemasViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    session: SessionRepository,
    offlineFlag: OfflineFlag,
    private val clock: Clock,
) : ViewModel() {
    val signedIn: StateFlow<Boolean?> = session.signedIn
    val offline: StateFlow<Boolean> = offlineFlag.offline
    private val _state = MutableStateFlow<UiState<CinemasUi>>(UiState.Loading)
    val state: StateFlow<UiState<CinemasUi>> = _state

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = catalog.cinemas()) {
                is ApiResult.Err -> UiState.Error(result.error)
                is ApiResult.Ok -> {
                    val cinemas = result.data
                    if (cinemas.isEmpty()) UiState.Empty()
                    // HCM first like the design's default city, otherwise whichever city comes first.
                    else UiState.Content(CinemasUi(cinemas, city = cinemas.map { it.city }.distinct().sorted().let { if ("HCM" in it) "HCM" else it.first() }))
                }
            }
            openFirst() // the design opens the first cinema of the city straight away, with today's showings
        }
    }

    fun selectCity(city: String) {
        update { it.copy(city = city, open = null) }
        openFirst()
    }

    private fun openFirst() {
        val first = current()?.visible?.firstOrNull() ?: return
        open(first.id)
    }

    /** Opening a card shows today's showings; closing it again keeps what was loaded. */
    fun toggle(cinemaId: Long) {
        val ui = current() ?: return
        if (ui.open == cinemaId) { update { it.copy(open = null) }; return }
        open(cinemaId)
    }

    private fun open(cinemaId: Long) {
        val ui = current() ?: return
        update { it.copy(open = cinemaId) }
        if (ui.shows[cinemaId] is UiState.Content) return
        update { it.copy(shows = it.shows + (cinemaId to UiState.Loading)) }
        viewModelScope.launch {
            val today = LocalDate.now(clock.withZone(CinemaZone))
            val state = when (val result = catalog.showtimesAtCinema(cinemaId, today)) {
                is ApiResult.Ok -> UiState.Content(result.data)
                is ApiResult.Err -> UiState.Error(result.error)
            }
            update { it.copy(shows = it.shows + (cinemaId to state)) }
        }
    }

    private fun current(): CinemasUi? = (_state.value as? UiState.Content)?.data

    private fun update(change: (CinemasUi) -> CinemasUi) {
        _state.update { s -> if (s is UiState.Content) UiState.Content(change(s.data)) else s }
    }
}
