package com.ceta.cinema.ui.screen.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.TicketRepository
import com.ceta.cinema.domain.model.Booking
import com.ceta.cinema.domain.usecase.PaymentOutcome
import com.ceta.cinema.domain.usecase.PaymentPolling
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentResultUi(val outcome: PaymentOutcome = PaymentOutcome.PROCESSING, val booking: Booking? = null)

/**
 * "Success" is shown only when the backend says PAID. Whatever the VNPay page or the deep link claimed is ignored: this
 * asks the server, at growing intervals for about a minute, then offers a manual re-check.
 */
@HiltViewModel
class PaymentResultViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val bookings: BookingRepository,
    private val tickets: TicketRepository,
) : ViewModel() {
    private val bookingId: Long = checkNotNull(savedState["bookingId"])
    private val _state = MutableStateFlow(PaymentResultUi())
    val state: StateFlow<PaymentResultUi> = _state
    private var polling: Job? = null

    init { startPolling() }

    fun checkAgain() {
        _state.update { it.copy(outcome = PaymentOutcome.PROCESSING) }
        startPolling()
    }

    private fun startPolling() {
        polling?.cancel()
        polling = viewModelScope.launch {
            for (seconds in listOf(0L) + PaymentPolling.DELAYS_SECONDS) { // look at once (the customer may already be back), then back off
                if (seconds > 0) delay(seconds * 1000)
                val booking = (bookings.get(bookingId) as? ApiResult.Ok)?.data ?: continue // a hiccup: try again at the next interval
                val outcome = PaymentPolling.outcome(booking)
                _state.update { it.copy(outcome = outcome, booking = booking) }
                if (outcome.terminal) {
                    bookings.forgetPending()
                    if (outcome == PaymentOutcome.PAID) tickets.forget() // the new tickets must show up in "My tickets" right away
                    return@launch
                }
            }
            _state.update { it.copy(outcome = PaymentOutcome.STILL_PENDING) }
        }
    }
}
