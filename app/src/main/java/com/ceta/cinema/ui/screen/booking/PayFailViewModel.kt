package com.ceta.cinema.ui.screen.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.domain.model.Booking
import com.ceta.cinema.domain.model.PaymentProvider
import com.ceta.cinema.domain.model.Showtime
import com.ceta.cinema.domain.usecase.HoldClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

data class PayFailUi(
    val bookingId: Long,
    val method: PaymentProvider,
    val booking: Booking? = null,
    val show: Showtime? = null,
    val secondsLeft: Long = 0,
    val warning: Boolean = false,
    val expired: Boolean = false,
    val working: Boolean = false,
    val error: ApiError? = null,
)

sealed interface PayFailEvent {
    /** Open the gateway again for the same method. */
    data class Retry(val url: String) : PayFailEvent
    /** The booking was cancelled and the seats released. */
    data object Cancelled : PayFailEvent
}

/**
 * Design item 10: the gateway declined. The order is still held and unpaid, so the seat countdown keeps running; the customer can
 * retry with the same method, go back and pick another, or cancel the booking.
 */
@HiltViewModel
class PayFailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val bookings: BookingRepository,
    private val catalog: CatalogRepository,
    clock: Clock,
) : ViewModel() {
    private val bookingId: Long = checkNotNull(savedState["bookingId"])
    private val hold = HoldClock { clock.instant() }
    private val _state = MutableStateFlow(PayFailUi(bookingId, bookings.lastProvider(bookingId)))
    val state: StateFlow<PayFailUi> = _state
    private val _events = Channel<PayFailEvent>(Channel.BUFFERED)
    val events: Flow<PayFailEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val booking = (bookings.get(bookingId) as? ApiResult.Ok)?.data ?: return@launch
            booking.holdExpiresAt?.let { hold.start(it) }
            hold.sync(booking.secondsRemaining) // the server's own count wins over the phone's clock
            _state.update { it.copy(booking = booking, secondsLeft = hold.remainingSeconds(), warning = hold.isWarning(), expired = hold.isExpired()) }
            booking.showtimeId?.let { id -> (catalog.showtime(id) as? ApiResult.Ok)?.data?.let { s -> _state.update { it.copy(show = s) } } }
        }
    }

    /** Once a second while the screen is visible. The countdown stands still while a call is in flight. */
    fun tick() = _state.update {
        if (it.working || it.booking == null) it else it.copy(secondsLeft = hold.remainingSeconds(), warning = hold.isWarning(), expired = hold.isExpired())
    }

    fun retry() {
        val ui = _state.value
        if (ui.working || ui.expired) return // a double tap opens one payment page
        _state.update { it.copy(working = true, error = null) }
        viewModelScope.launch {
            when (val result = bookings.paymentUrl(bookingId, ui.method)) {
                is ApiResult.Ok -> { _state.update { it.copy(working = false) }; _events.send(PayFailEvent.Retry(result.data)) }
                is ApiResult.Err -> _state.update { it.copy(working = false, error = result.error, expired = result.error.code == "HOLD_EXPIRED" || it.expired) }
            }
        }
    }

    fun cancelBooking() {
        if (_state.value.working) return
        _state.update { it.copy(working = true, error = null) }
        viewModelScope.launch {
            when (val result = bookings.cancel(bookingId)) {
                is ApiResult.Ok -> { _state.update { it.copy(working = false) }; _events.send(PayFailEvent.Cancelled) }
                is ApiResult.Err -> _state.update { it.copy(working = false, error = result.error) }
            }
        }
    }
}
