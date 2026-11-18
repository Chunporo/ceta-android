package com.ceta.cinema.ui.screen.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.domain.model.BookingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VietQrUi(val imageUrl: String, val note: String, val amount: Long = 0, val paid: Boolean = false, val expired: Boolean = false)

/** Waits for the bank transfer: the server learns about it from the bank feed, so the app just asks the booking every few seconds. */
@HiltViewModel
class VietQrViewModel @Inject constructor(savedState: SavedStateHandle, private val bookings: BookingRepository) : ViewModel() {
    private val bookingId: Long = checkNotNull(savedState["bookingId"])
    val bookingIdValue: Long get() = bookingId
    private val _state = MutableStateFlow(VietQrUi(savedState.get<String>("img").orEmpty(), savedState.get<String>("note").orEmpty()))
    val state: StateFlow<VietQrUi> = _state

    init {
        viewModelScope.launch {
            while (isActive) {
                val booking = (bookings.get(bookingId) as? ApiResult.Ok)?.data
                if (booking != null) {
                    _state.update { it.copy(amount = booking.totalAmount) }
                    when (booking.status) {
                        BookingStatus.PAID -> { _state.update { it.copy(paid = true) }; return@launch }
                        BookingStatus.PENDING -> Unit
                        else -> { _state.update { it.copy(expired = true) }; return@launch }
                    }
                }
                delay(3_000)
            }
        }
    }
}
