package com.ceta.cinema.ui.screen.showtime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.CinemaZone
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.domain.model.ShowFormat
import com.ceta.cinema.domain.model.Showtime
import com.ceta.cinema.domain.usecase.ShowtimeGrouping
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class ShowtimePickerUi(
    val week: List<LocalDate>,
    val date: LocalDate,
    val format: ShowFormat? = null,           // null = all formats
    val byCinema: Boolean = true,
    val shows: UiState<List<Showtime>> = UiState.Loading,
)

@HiltViewModel
class ShowtimePickerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val clock: Clock,
) : ViewModel() {
    private val movieId: String = checkNotNull(savedState["movieId"])
    private val today = LocalDate.now(clock.withZone(CinemaZone))
    private val _state = MutableStateFlow(ShowtimePickerUi(week = ShowtimeGrouping.week(today), date = today))
    val state: StateFlow<ShowtimePickerUi> = _state
    private var job: Job? = null

    init { load() }

    fun selectDate(date: LocalDate) {
        if (date == _state.value.date) return
        _state.update { it.copy(date = date) }
        load()
    }

    fun selectFormat(format: ShowFormat?) = _state.update { it.copy(format = format) }

    fun toggleView() = _state.update { it.copy(byCinema = !it.byCinema) }

    fun load() {
        _state.update { it.copy(shows = UiState.Loading) }
        val date = _state.value.date
        job?.cancel()
        job = viewModelScope.launch {
            val result = catalog.showtimesForMovie(movieId, date)
            // An answer for a date the customer has already left must not overwrite the new one.
            if (date != _state.value.date) return@launch
            _state.update {
                it.copy(shows = when (result) {
                    is ApiResult.Ok -> UiState.Content(result.data)
                    is ApiResult.Err -> UiState.Error(result.error)
                })
            }
        }
    }
}
