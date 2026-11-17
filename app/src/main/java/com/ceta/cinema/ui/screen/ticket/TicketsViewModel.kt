package com.ceta.cinema.ui.screen.ticket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.OfflineFlag
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.data.repository.TicketRepository
import com.ceta.cinema.domain.usecase.TicketGroup
import com.ceta.cinema.domain.usecase.TicketGrouping
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class TicketsData(val upcoming: List<TicketGroup>, val watched: List<TicketGroup>)

@HiltViewModel
class TicketsViewModel @Inject constructor(
    private val tickets: TicketRepository,
    session: SessionRepository,
    offlineFlag: OfflineFlag,
    private val clock: Clock,
) : ViewModel() {
    val signedIn: StateFlow<Boolean?> = session.signedIn
    val offline: StateFlow<Boolean> = offlineFlag.offline
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
                    val (upcoming, watched) = TicketGrouping.split(TicketGrouping.group(result.data), clock.instant())
                    UiState.Content(TicketsData(upcoming, watched))
                }
            }
        }
    }
}
