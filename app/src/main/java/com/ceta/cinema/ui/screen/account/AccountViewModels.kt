package com.ceta.cinema.ui.screen.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.local.prefs.SettingsStore
import com.ceta.cinema.data.repository.AccountRepository
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.domain.model.AppNotification
import com.ceta.cinema.domain.model.BookingSummary
import com.ceta.cinema.domain.model.Loyalty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val account: AccountRepository,
    session: SessionRepository,
    settings: SettingsStore,
) : ViewModel() {
    val signedIn: StateFlow<Boolean?> = session.signedIn
    val displayName: StateFlow<String?> = settings.settings.map { it.displayName }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _loyalty = MutableStateFlow<UiState<Loyalty>>(UiState.Loading)
    val loyalty: StateFlow<UiState<Loyalty>> = _loyalty
    private val _contact = MutableStateFlow<String?>(null)
    /** The phone number (shown as +84 912 345 678) or the email under the name, as in the design; null until known. */
    val contact: StateFlow<String?> = _contact

    init { load() }

    fun load() {
        if (signedIn.value != true) return
        _loyalty.value = UiState.Loading
        viewModelScope.launch {
            (account.profile() as? ApiResult.Ok)?.data?.let { p -> _contact.value = p.phone?.let(com.ceta.cinema.core.PhoneNumber::display) ?: p.email }
        }
        viewModelScope.launch {
            _loyalty.value = when (val r = account.loyalty()) {
                is ApiResult.Ok -> UiState.Content(r.data)
                is ApiResult.Err -> UiState.Error(r.error)
            }
        }
    }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(private val bookings: BookingRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<BookingSummary>>>(UiState.Loading)
    val state: StateFlow<UiState<List<BookingSummary>>> = _state

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = bookings.history()) {
                is ApiResult.Err -> UiState.Error(r.error)
                is ApiResult.Ok -> if (r.data.isEmpty()) UiState.Empty() else UiState.Content(r.data)
            }
        }
    }
}

data class NotificationsUi(val items: List<AppNotification>, val problem: ApiError? = null) {
    val unread: Int get() = items.count { !it.read }
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val account: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<NotificationsUi>>(UiState.Loading)
    val state: StateFlow<UiState<NotificationsUi>> = _state

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = account.notifications()) {
                is ApiResult.Err -> UiState.Error(r.error)
                is ApiResult.Ok -> if (r.data.isEmpty()) UiState.Empty() else UiState.Content(NotificationsUi(r.data))
            }
        }
    }

    /** Opening one marks it read at once; if the server refuses it goes back to unread. */
    fun markRead(id: Long) {
        val ui = current() ?: return
        if (ui.items.firstOrNull { it.id == id }?.read != false) return
        update { it.copy(items = it.items.map { n -> if (n.id == id) n.copy(read = true) else n }) }
        viewModelScope.launch {
            if (account.markRead(id) is ApiResult.Err) update { it.copy(items = it.items.map { n -> if (n.id == id) n.copy(read = false) else n }) }
        }
    }

    fun markAllRead() {
        val ui = current() ?: return
        if (ui.unread == 0) return
        val before = ui.items
        update { it.copy(items = it.items.map { n -> n.copy(read = true) }) }
        viewModelScope.launch {
            val r = account.markAllRead()
            if (r is ApiResult.Err) update { it.copy(items = before, problem = r.error) }
        }
    }

    private fun current(): NotificationsUi? = (_state.value as? UiState.Content)?.data

    private fun update(change: (NotificationsUi) -> NotificationsUi) {
        _state.update { s -> if (s is UiState.Content) UiState.Content(change(s.data)) else s }
    }
}
