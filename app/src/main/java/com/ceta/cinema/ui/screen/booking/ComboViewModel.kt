package com.ceta.cinema.ui.screen.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.domain.model.Booking
import com.ceta.cinema.domain.model.Combo
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

data class ComboUi(
    val booking: Booking,
    val combos: List<Combo>,
    val quantities: Map<Long, Int>,
    val secondsLeft: Long,
    val warning: Boolean,
    val expired: Boolean,
    val saving: Boolean = false,
    val error: ApiError? = null,
) {
    val snacksTotal: Long get() = combos.sumOf { (quantities[it.id] ?: 0) * it.price }
    val total: Long get() = booking.seatsAmount + snacksTotal - booking.discountAmount - booking.pointsAmount
}

/** The snacks step between the seat map and checkout. It can be skipped, and it keeps the seats' countdown running. */
@HiltViewModel
class ComboViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val bookings: BookingRepository,
    private val catalog: CatalogRepository,
    clock: Clock,
) : ViewModel() {
    private val bookingId: Long = checkNotNull(savedState["bookingId"])
    private val hold = HoldClock { clock.instant() }
    private val _state = MutableStateFlow<UiState<ComboUi>>(UiState.Loading)
    val state: StateFlow<UiState<ComboUi>> = _state
    private val _next = Channel<Unit>(Channel.BUFFERED)
    /** Fires when the extras are saved and checkout should open. */
    val next: Flow<Unit> = _next.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val booking = bookings.get(bookingId)
            val combos = catalog.combos()
            when {
                booking is ApiResult.Err -> _state.value = UiState.Error(booking.error)
                else -> {
                    val b = (booking as ApiResult.Ok).data
                    b.holdExpiresAt?.let { hold.start(it) }
                    hold.sync(b.secondsRemaining) // the server's own count wins over the phone's clock
                    _state.value = UiState.Content(ComboUi(
                        booking = b,
                        // Snacks are an extra: if the menu cannot load, the customer can still skip to checkout.
                        combos = (combos as? ApiResult.Ok)?.data.orEmpty(),
                        quantities = b.combos.associate { it.comboId to it.quantity },
                        secondsLeft = hold.remainingSeconds(), warning = hold.isWarning(), expired = hold.isExpired(),
                    ))
                }
            }
        }
    }

    fun change(comboId: Long, delta: Int) = update {
        val next = ((it.quantities[comboId] ?: 0) + delta).coerceIn(0, MAX_PER_COMBO)
        it.copy(quantities = it.quantities + (comboId to next), error = null)
    }

    /** Saves the snacks (or none, when skipping) and moves on. The voucher and points already chosen are kept. */
    fun proceed(skip: Boolean) {
        val ui = current() ?: return
        if (ui.saving || ui.expired) return
        update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val result = bookings.updateExtras(bookingId, if (skip) emptyMap() else ui.quantities, ui.booking.voucherCode, ui.booking.pointsUsed > 0)
            when {
                result is ApiResult.Ok -> { update { it.copy(saving = false) }; _next.send(Unit) }
                // Payment already started: the order is final, just go and pay it.
                (result as ApiResult.Err).error.code == "PAYMENT_ALREADY_STARTED" -> { update { it.copy(saving = false) }; _next.send(Unit) }
                else -> update { it.copy(saving = false, error = result.error, expired = result.error.code == "HOLD_EXPIRED" || it.expired) }
            }
        }
    }

    /** Called once a second by the screen while it is visible (no background loop lives in the ViewModel). */
    fun tick() = update { it.copy(secondsLeft = hold.remainingSeconds(), warning = hold.isWarning(), expired = hold.isExpired()) }

    private fun current(): ComboUi? = (_state.value as? UiState.Content)?.data

    private fun update(change: (ComboUi) -> ComboUi) {
        _state.update { s -> if (s is UiState.Content) UiState.Content(change(s.data)) else s }
    }

    companion object { const val MAX_PER_COMBO = 10 }
}
