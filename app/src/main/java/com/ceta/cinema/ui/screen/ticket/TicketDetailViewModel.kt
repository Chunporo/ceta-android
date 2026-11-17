package com.ceta.cinema.ui.screen.ticket

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.TicketRepository
import com.ceta.cinema.domain.model.ComboLine
import com.ceta.cinema.domain.model.Ticket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketDetailUi(val tickets: List<Ticket>, val combos: List<ComboLine>)

@HiltViewModel
class TicketDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val tickets: TicketRepository,
    private val bookings: BookingRepository,
) : ViewModel() {
    private val bookingId: Long = checkNotNull(savedState["bookingId"])
    private val _state = MutableStateFlow<UiState<TicketDetailUi>>(UiState.Loading)
    val state: StateFlow<UiState<TicketDetailUi>> = _state

    init { load() }

    /** Always asks the server (or the 5 minute copy): a ticket may have been used at the door since it was last seen. */
    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            when (val result = tickets.myTickets()) {
                is ApiResult.Err -> _state.value = UiState.Error(result.error)
                is ApiResult.Ok -> {
                    val mine = result.data.filter { it.bookingId == bookingId }.sortedBy { it.seatLabel }
                    if (mine.isEmpty()) { _state.value = UiState.Error(ApiError("TICKET_NOT_FOUND", 404)); return@launch }
                    // The snacks are on the order, not on the tickets: fetched separately, and optional for showing the QR.
                    val combos = (bookings.get(bookingId) as? ApiResult.Ok)?.data?.combos.orEmpty()
                    _state.value = UiState.Content(TicketDetailUi(mine, combos))
                }
            }
        }
    }
}
