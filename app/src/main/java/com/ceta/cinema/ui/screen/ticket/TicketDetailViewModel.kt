package com.ceta.cinema.ui.screen.ticket

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.TicketRepository
import com.ceta.cinema.domain.model.BookingStatus
import com.ceta.cinema.domain.model.ComboLine
import com.ceta.cinema.domain.usecase.CancelPolicy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.time.Clock
import com.ceta.cinema.domain.model.Ticket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TicketDetailUi(
    val tickets: List<Ticket>, val combos: List<ComboLine>,
    /** The order was cancelled and refunded: the QR is struck out. */
    val cancelled: Boolean = false,
    /** Offer "Cancel ticket": a PAID order, nothing scanned yet and at least 2 hours before the show. */
    val canCancel: Boolean = false,
    val refund: Long = 0,
    val cancelling: Boolean = false,
    val cancelError: ApiError? = null,
)

sealed interface TicketDetailEvent {
    /** The order was cancelled: leave for the Cancelled tab. */
    data object Cancelled : TicketDetailEvent
}

@HiltViewModel
class TicketDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val tickets: TicketRepository,
    private val bookings: BookingRepository,
    private val clock: Clock,
) : ViewModel() {
    private val bookingId: Long = checkNotNull(savedState["bookingId"])
    private val _state = MutableStateFlow<UiState<TicketDetailUi>>(UiState.Loading)
    val state: StateFlow<UiState<TicketDetailUi>> = _state

    private val _events = Channel<TicketDetailEvent>(Channel.BUFFERED)
    val events: Flow<TicketDetailEvent> = _events.receiveAsFlow()

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
                    val order = (bookings.get(bookingId) as? ApiResult.Ok)?.data
                    val cancelled = order?.status == BookingStatus.REFUNDED
                    val canCancel = order?.status == BookingStatus.PAID && CancelPolicy.canCancel(mine.first().start, clock.instant(), mine.any { it.used })
                    _state.value = UiState.Content(TicketDetailUi(mine, order?.combos.orEmpty(), cancelled, canCancel, CancelPolicy.refundFor(order?.totalAmount ?: mine.sumOf { it.price })))
                }
            }
        }
    }

    /** Cancels the whole order (all its seats). */
    fun cancel() {
        val ui = (_state.value as? UiState.Content)?.data ?: return
        if (ui.cancelling || !ui.canCancel) return // a double tap cancels once
        _state.value = UiState.Content(ui.copy(cancelling = true, cancelError = null))
        viewModelScope.launch {
            when (val result = bookings.cancelPaid(bookingId)) {
                is ApiResult.Ok -> { tickets.forget(); _state.value = UiState.Content(ui.copy(cancelling = false, canCancel = false, cancelled = true)); _events.send(TicketDetailEvent.Cancelled) }
                is ApiResult.Err -> _state.value = UiState.Content(ui.copy(cancelling = false, cancelError = result.error))
            }
        }
    }

    fun consumeCancelError() { (_state.value as? UiState.Content)?.data?.let { _state.value = UiState.Content(it.copy(cancelError = null)) } }
}
