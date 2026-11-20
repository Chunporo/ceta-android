package com.ceta.cinema.ui.screen.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.OfflineFlag
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.local.prefs.SettingsStore
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.data.repository.TicketRepository
import com.ceta.cinema.domain.model.BookingStatus
import com.ceta.cinema.domain.usecase.CancelledTicket
import com.ceta.cinema.domain.usecase.TicketGroup
import com.ceta.cinema.domain.usecase.TicketGrouping
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class TicketsData(val upcoming: List<TicketGroup>, val watched: List<TicketGroup>, val cancelled: List<CancelledTicket> = emptyList())

@HiltViewModel
class TicketsViewModel @Inject constructor(
    private val tickets: TicketRepository,
    private val bookings: BookingRepository,
    private val settings: SettingsStore,
    session: SessionRepository,
    offlineFlag: OfflineFlag,
    private val clock: Clock,
) : ViewModel() {
    val signedIn: StateFlow<Boolean?> = session.signedIn
    val offline: StateFlow<Boolean> = offlineFlag.offline
    /** The stars given to watched shows, by ticket code. */
    val ratings: StateFlow<Map<String, Int>> = settings.settings.map { it.ticketRatings }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun rate(code: String, stars: Int) { viewModelScope.launch { settings.setTicketRating(code, stars) } }

    private val _state = MutableStateFlow<UiState<TicketsData>>(UiState.Loading)
    val state: StateFlow<UiState<TicketsData>> = _state

    init { load() }

    fun load(forceRefresh: Boolean = false) {
        if (signedIn.value != true) return // a guest has no tickets: the screen asks them to sign in
        if (_state.value !is UiState.Content) _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = tickets.myTickets(forceRefresh)) {
                is ApiResult.Err -> UiState.Error(result.error)
                is ApiResult.Ok -> {
                    // A cancelled order keeps its tickets on the server; its status lives on the order, so ask the order history.
                    val refunded = (bookings.history() as? ApiResult.Ok)?.data.orEmpty().filter { it.status == BookingStatus.REFUNDED }.associate { it.id to it.totalAmount }
                    val (gone, live) = TicketGrouping.group(result.data).partition { it.bookingId in refunded }
                    val (upcoming, watched) = TicketGrouping.split(live, clock.instant())
                    val cancelled = gone.sortedByDescending { it.lead.start }.map { CancelledTicket(it, refunded.getValue(it.bookingId)) }
                    UiState.Content(TicketsData(upcoming, watched, cancelled))
                }
            }
        }
    }
}
