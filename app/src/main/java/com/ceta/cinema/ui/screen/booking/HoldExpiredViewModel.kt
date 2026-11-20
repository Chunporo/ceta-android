package com.ceta.cinema.ui.screen.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.domain.model.Showtime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HoldExpiredUi(val showtimeId: Long, val seats: List<String>, val showtime: Showtime? = null)

/** Reads which show and which seats the lapsed hold was for (both come in the route), and loads the show for its poster and title. */
@HiltViewModel
class HoldExpiredViewModel @Inject constructor(savedState: SavedStateHandle, private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(
        HoldExpiredUi(
            showtimeId = checkNotNull(savedState["showtimeId"]),
            seats = savedState.get<String>("seats").orEmpty().split(',').filter { it.isNotBlank() },
        ),
    )
    val state: StateFlow<HoldExpiredUi> = _state

    init {
        viewModelScope.launch {
            (catalog.showtime(_state.value.showtimeId) as? ApiResult.Ok)?.let { r -> _state.value = _state.value.copy(showtime = r.data) }
        }
    }
}
