package com.ceta.cinema.ui.screen.seat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.data.repository.SeatRepository
import com.ceta.cinema.domain.model.Seat
import com.ceta.cinema.domain.model.Showtime
import com.ceta.cinema.domain.usecase.HoldClock
import com.ceta.cinema.domain.usecase.LoneSeats
import com.ceta.cinema.domain.usecase.SeatSelection
import java.time.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SeatMessage {
    data object MaxSeats : SeatMessage
    data object CouplePartnerTaken : SeatMessage
    data class SeatsTaken(val labels: List<String>) : SeatMessage
    data class Failed(val error: ApiError) : SeatMessage
    /** Two minutes left of the 10 minutes since the first seat was chosen. */
    data object HoldWarning : SeatMessage
    /** Ten minutes passed: the chosen seats were cleared. */
    data object HoldExpired : SeatMessage
}

data class SeatMapUi(
    val seats: List<Seat>,
    val showtime: Showtime? = null,
    val selected: Set<Long> = emptySet(),
    /** Seats the server refused with 409: drawn with a warning outline until the customer chooses again. */
    val conflicts: Set<Long> = emptySet(),
    val submitting: Boolean = false,
    val message: SeatMessage? = null,
    /** The countdown since the first chosen seat; null while no seat is chosen. */
    val holdSecondsLeft: Long? = null,
    val holdWarning: Boolean = false,
) {
    /** Free seats the current choice would leave alone (soft warning, never blocks). */
    val loneSeats: List<Seat> get() = LoneSeats.find(seats, selected)
    val selectedSeats: List<Seat> get() = seats.filter { it.id in selected }.sortedWith(compareBy({ it.row }, { it.column }))
    val total: Long get() = selectedSeats.sumOf { it.price }
}

@HiltViewModel
class SeatMapViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val seatRepository: SeatRepository,
    private val catalog: CatalogRepository,
    private val bookings: BookingRepository,
    clock: Clock,
) : ViewModel() {
    private val hold = HoldClock { clock.instant() }
    private var warned = false
    private val showtimeId: Long = checkNotNull(savedState["showtimeId"])
    private val rules = SeatSelection()
    private val _state = MutableStateFlow<UiState<SeatMapUi>>(UiState.Loading)
    val state: StateFlow<UiState<SeatMapUi>> = _state

    private val _booked = Channel<Long>(Channel.BUFFERED)
    /** The id of the booking just created: the screen moves on to checkout. */
    val booked: Flow<Long> = _booked.receiveAsFlow()

    init {
        load()
        // Checkout or snacks ran out of time and sent the customer back here: start again with no seat chosen.
        viewModelScope.launch {
            savedState.getStateFlow(HOLD_EXPIRED_KEY, false).collect { expired ->
                if (expired) { savedState[HOLD_EXPIRED_KEY] = false; clearSelection(SeatMessage.HoldExpired) }
            }
        }
    }

    /** The seat map is never cached, so every load and every refresh asks the server. */
    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val show = (catalog.showtime(showtimeId) as? ApiResult.Ok)?.data
            when (val result = seatRepository.seats(showtimeId)) {
                is ApiResult.Ok -> _state.value = UiState.Content(SeatMapUi(result.data, show))
                is ApiResult.Err -> _state.value = UiState.Error(result.error)
            }
        }
    }

    /** Quiet refresh while the screen is visible: keeps the map close to live and drops seats someone else just took. */
    fun refresh() {
        val ui = current() ?: return
        if (ui.submitting) return
        viewModelScope.launch {
            val result = seatRepository.seats(showtimeId) as? ApiResult.Ok ?: return@launch // a failed refresh keeps what is shown
            update { latest ->
                val (kept, lost) = rules.reconcile(result.data, latest.selected)
                if (kept.isEmpty() && hold.isRunning()) { hold.stop(); warned = false } // the last chosen seat was taken
                latest.copy(
                    seats = result.data, selected = kept, conflicts = latest.conflicts.filter { id -> result.data.any { it.id == id && !it.selectable } }.toSet(),
                    message = if (lost.isNotEmpty()) SeatMessage.SeatsTaken(lost.map { it.label }) else latest.message,
                ).withHold()
            }
        }
    }

    fun onSeat(seatId: Long) {
        val ui = current() ?: return
        if (ui.submitting) return
        when (val outcome = rules.toggle(ui.seats, ui.selected, seatId)) {
            is SeatSelection.Outcome.Changed -> {
                // The countdown starts with the first chosen seat and ends when none is left.
                if (ui.selected.isEmpty() && outcome.selected.isNotEmpty()) { hold.startFor(HoldClock.HOLD); warned = false }
                if (outcome.selected.isEmpty()) { hold.stop(); warned = false }
                update { it.copy(selected = outcome.selected, conflicts = it.conflicts - outcome.selected, message = null).withHold() }
            }
            SeatSelection.Outcome.TooMany -> update { it.copy(message = SeatMessage.MaxSeats) }
            SeatSelection.Outcome.CouplePartnerTaken -> update { it.copy(message = SeatMessage.CouplePartnerTaken) }
            SeatSelection.Outcome.Ignored -> Unit
        }
    }

    fun consumeMessage() = update { it.copy(message = null) }

    /** Called once a second while the screen is visible. */
    fun tick() {
        if (!hold.isRunning()) return
        if (hold.isExpired()) { clearSelection(SeatMessage.HoldExpired); return }
        update {
            val next = it.withHold()
            if (next.holdWarning && !warned) { warned = true; next.copy(message = SeatMessage.HoldWarning) } else next
        }
    }

    private fun clearSelection(message: SeatMessage) {
        hold.stop(); warned = false
        update { it.copy(selected = emptySet(), conflicts = emptySet(), message = message, holdSecondsLeft = null, holdWarning = false) }
    }

    private fun SeatMapUi.withHold() = if (hold.isRunning()) copy(holdSecondsLeft = hold.remainingSeconds(), holdWarning = hold.isWarning())
        else copy(holdSecondsLeft = null, holdWarning = false)

    fun continueBooking() {
        val ui = current() ?: return
        if (ui.submitting || ui.selected.isEmpty()) return // a double tap creates one booking, not two
        update { it.copy(submitting = true, message = null) }
        viewModelScope.launch {
            when (val result = bookings.create(showtimeId, ui.selectedSeats.map { it.id })) {
                is ApiResult.Ok -> {
                    update { it.copy(submitting = false) }
                    _booked.send(result.data.bookingId)
                }
                is ApiResult.Err -> {
                    val taken = result.error.conflictSeatIds.toSet()
                    update {
                        it.copy(
                            submitting = false, conflicts = taken, selected = it.selected - taken,
                            message = if (taken.isNotEmpty()) SeatMessage.Failed(result.error) else SeatMessage.Failed(result.error),
                        )
                    }
                    if (taken.isNotEmpty()) refresh() // show the new truth right away
                }
            }
        }
    }

    companion object { const val HOLD_EXPIRED_KEY = "holdExpired" }

    private fun current(): SeatMapUi? = (_state.value as? UiState.Content)?.data

    private fun update(change: (SeatMapUi) -> SeatMapUi) {
        _state.update { s -> if (s is UiState.Content) UiState.Content(change(s.data)) else s }
    }
}
