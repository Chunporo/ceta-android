package com.ceta.cinema.ui.screen.booking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.repository.AccountRepository
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.domain.model.Booking
import com.ceta.cinema.domain.model.Loyalty
import com.ceta.cinema.domain.model.PaymentProvider
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

data class CheckoutUi(
    val booking: Booking,
    val loyalty: Loyalty?,
    val voucherInput: String = "",
    /** Why the last voucher was refused: shown under the box, the earlier valid voucher stays applied. */
    val voucherError: ApiError? = null,
    val secondsLeft: Long = 0,
    val warning: Boolean = false,
    val expired: Boolean = false,
    /** True while a voucher/points change or the payment link is being fetched: buttons are off so nothing is sent twice. */
    val working: Boolean = false,
    val error: ApiError? = null,
    /** What the server offers (VNPay when it cannot be asked) and the one chosen. */
    val methods: List<PaymentProvider> = listOf(PaymentProvider.VNPAY),
    val method: PaymentProvider = PaymentProvider.VNPAY,
) {
    val usingPoints: Boolean get() = booking.pointsUsed > 0
    val canUsePoints: Boolean get() = loyalty?.canRedeem == true
    val canPay: Boolean get() = !expired && !working && booking.status.name == "PENDING"
}

sealed interface CheckoutEvent {
    /** Open the payment page (VNPay or MoMo) in a Custom Tab, then wait for the result on the next screen. */
    data class OpenPayment(val url: String, val bookingId: Long) : CheckoutEvent

    /** VietQR is a bank transfer, not a web page: show the QR image and the transfer note, and wait there. */
    data class ShowVietQr(val imageUrl: String, val note: String, val bookingId: Long) : CheckoutEvent
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val bookings: BookingRepository,
    private val account: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val bookingId: Long = checkNotNull(savedState["bookingId"])
    private val hold = HoldClock { clock.instant() }
    private val _state = MutableStateFlow<UiState<CheckoutUi>>(UiState.Loading)
    val state: StateFlow<UiState<CheckoutUi>> = _state
    private val _events = Channel<CheckoutEvent>(Channel.BUFFERED)
    val events: Flow<CheckoutEvent> = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            when (val booking = bookings.get(bookingId)) {
                is ApiResult.Err -> _state.value = UiState.Error(booking.error)
                is ApiResult.Ok -> {
                    val b = booking.data
                    b.holdExpiresAt?.let { hold.start(it) }
                    hold.sync(b.secondsRemaining)
                    // Points need an account and a network; without them the page still works, just without that option.
                    val loyalty = (account.loyalty() as? ApiResult.Ok)?.data
                    val methods = (bookings.paymentMethods() as? ApiResult.Ok)?.data?.takeIf { PaymentProvider.VNPAY in it } ?: listOf(PaymentProvider.VNPAY)
                    _state.value = UiState.Content(CheckoutUi(b, loyalty, voucherInput = b.voucherCode.orEmpty(), methods = methods,
                        secondsLeft = hold.remainingSeconds(), warning = hold.isWarning(), expired = hold.isExpired()))
                }
            }
        }
    }

    fun selectMethod(method: PaymentProvider) = update { if (method in it.methods && !it.working) it.copy(method = method) else it }

    fun onVoucher(text: String) = update { it.copy(voucherInput = text.uppercase().take(30), voucherError = null) }

    fun applyVoucher() {
        val ui = current() ?: return
        if (ui.working || ui.expired || ui.voucherInput.isBlank()) return
        change(ui, voucher = ui.voucherInput, usePoints = ui.usingPoints) { err -> copy(voucherError = err) }
    }

    fun removeVoucher() {
        val ui = current() ?: return
        if (ui.working || ui.expired) return
        change(ui, voucher = null, usePoints = ui.usingPoints) { err -> copy(error = err) }
    }

    fun togglePoints() {
        val ui = current() ?: return
        if (ui.working || ui.expired) return
        change(ui, voucher = ui.booking.voucherCode, usePoints = !ui.usingPoints) { err -> copy(error = err) }
    }

    /** One call replaces snacks, voucher and points together: send the whole desired state, then re-read the order. */
    private fun change(ui: CheckoutUi, voucher: String?, usePoints: Boolean, onError: CheckoutUi.(ApiError) -> CheckoutUi) {
        update { it.copy(working = true, error = null, voucherError = null) }
        viewModelScope.launch {
            val combos = ui.booking.combos.associate { it.comboId to it.quantity }
            when (val result = bookings.updateExtras(bookingId, combos, voucher, usePoints)) {
                is ApiResult.Err -> update { it.copy(working = false).onError(result.error) }
                is ApiResult.Ok -> {
                    val fresh = (bookings.get(bookingId) as? ApiResult.Ok)?.data
                    val loyalty = (account.loyalty() as? ApiResult.Ok)?.data
                    update { it.copy(working = false, booking = fresh ?: it.booking, loyalty = loyalty ?: it.loyalty, voucherInput = fresh?.voucherCode.orEmpty()) }
                }
            }
        }
    }

    fun pay() {
        val ui = current() ?: return
        if (!ui.canPay) return // a double tap opens one payment page, not two
        update { it.copy(working = true, error = null) }
        viewModelScope.launch {
            when (val result = bookings.paymentUrl(bookingId, ui.method)) {
                is ApiResult.Ok -> {
                    update { it.copy(working = false) }
                    _events.send(
                        if (ui.method == PaymentProvider.VIETQR) CheckoutEvent.ShowVietQr(result.data, transferNote(result.data), bookingId)
                        else CheckoutEvent.OpenPayment(result.data, bookingId),
                    )
                }
                is ApiResult.Err -> update { it.copy(working = false, error = result.error, expired = result.error.code == "HOLD_EXPIRED" || it.expired) }
            }
        }
    }

    /** Called once a second by the screen while it is visible (no background loop lives in the ViewModel). */
    /** The countdown stands still while a payment is being processed (design item 3). */
    fun tick() = update { if (it.working) it else it.copy(secondsLeft = hold.remainingSeconds(), warning = hold.isWarning(), expired = hold.isExpired()) }

    private fun current(): CheckoutUi? = (_state.value as? UiState.Content)?.data

    private fun update(change: (CheckoutUi) -> CheckoutUi) {
        _state.update { s -> if (s is UiState.Content) UiState.Content(change(s.data)) else s }
    }
}

/** The transfer note the server baked into the QR (`addInfo=CETA5A1`): the customer can also type it by hand. */
internal fun transferNote(imageUrl: String): String =
    Regex("[?&]addInfo=([^&]+)").find(imageUrl)?.groupValues?.get(1)?.let { java.net.URLDecoder.decode(it, "UTF-8") }.orEmpty()
