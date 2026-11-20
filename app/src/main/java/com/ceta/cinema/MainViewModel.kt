package com.ceta.cinema

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.SessionEvents
import com.ceta.cinema.data.local.prefs.AppSettings
import com.ceta.cinema.data.local.prefs.SettingsStore
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.ui.screen.main.Tab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val session: SessionRepository,
    private val settingsStore: SettingsStore,
    events: SessionEvents,
) : ViewModel() {
    val signedIn: StateFlow<Boolean?> = session.signedIn

    /** Null until the stored settings have been read, so the first frame is never drawn in the wrong theme or language. */
    val settings: StateFlow<AppSettings?> = settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _linkError = MutableStateFlow<ApiError?>(null)
    val linkError: StateFlow<ApiError?> = _linkError

    private val _requestedTab = MutableStateFlow<Tab?>(null)
    /** A screen can ask the shell to land on a tab (for example My tickets after a purchase). */
    val requestedTab: StateFlow<Tab?> = _requestedTab
    fun openTab(tab: Tab) { _requestedTab.value = tab }
    fun tabShown() { _requestedTab.value = null }

    private val _focusCinema = MutableStateFlow<Long?>(null)
    /** A cinema picked elsewhere (a search result): the Cinemas tab opens its card. */
    val focusCinema: StateFlow<Long?> = _focusCinema
    fun focusCinema(id: Long) { _focusCinema.value = id }
    fun cinemaFocused() { _focusCinema.value = null }

    private val _cancelledNotice = MutableStateFlow(false)
    /** A paid ticket was just cancelled: My tickets opens on the Cancelled tab and says so once. */
    val cancelledNotice: StateFlow<Boolean> = _cancelledNotice
    fun ticketCancelled() { _cancelledNotice.value = true }
    fun cancelledNoticeShown() { _cancelledNotice.value = false }

    data class PaymentReturn(val bookingId: Long, val declined: Boolean)

    private val _paymentReturn = Channel<PaymentReturn>(Channel.BUFFERED)
    /** The customer came back from the payment page: the order to ask the server about, and whether the link said it was declined. */
    val paymentReturn: Flow<PaymentReturn> = _paymentReturn.receiveAsFlow()

    /** The deep link only says "back from payment"; which order it was comes from what was saved when it was created. */
    fun onPaymentReturn(declined: Boolean = false) {
        viewModelScope.launch { settingsStore.settings.first().pendingBookingId?.let { _paymentReturn.send(PaymentReturn(it, declined)) } }
    }

    init {
        viewModelScope.launch { session.restore() }
        viewModelScope.launch { events.expired.collect { session.onSessionExpired() } }
    }

    fun onLink(link: String) {
        viewModelScope.launch {
            _linkError.value = (session.completeEmailLink(link) as? ApiResult.Err)?.error
        }
    }

    fun signOut() {
        viewModelScope.launch { session.signOut() }
    }

    fun setDark(dark: Boolean) { viewModelScope.launch { settingsStore.setDark(dark) } }
    fun setLanguage(language: String) { viewModelScope.launch { settingsStore.setLanguage(language) } }
    fun setGuest(guest: Boolean) { viewModelScope.launch { settingsStore.setGuestMode(guest) } }
    fun finishOnboarding() { viewModelScope.launch { settingsStore.setOnboardingSeen() } }
}
