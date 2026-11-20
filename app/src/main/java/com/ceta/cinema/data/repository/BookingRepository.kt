package com.ceta.cinema.data.repository

import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.safeApiCall
import com.ceta.cinema.data.local.prefs.SettingsStore
import com.ceta.cinema.data.remote.api.CetaApi
import com.ceta.cinema.data.remote.dto.BookingExtrasRequest
import com.ceta.cinema.data.remote.dto.ComboLineRequest
import com.ceta.cinema.data.remote.dto.CreateBookingRequest
import com.ceta.cinema.data.remote.dto.PaymentUrlRequest
import com.ceta.cinema.domain.model.PaymentProvider
import com.ceta.cinema.data.remote.mapper.toDomain
import com.ceta.cinema.domain.model.Booking
import com.ceta.cinema.domain.model.BookingSummary
import com.ceta.cinema.domain.model.CreatedBooking
import com.ceta.cinema.domain.model.Extras
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(private val api: CetaApi, private val settings: SettingsStore) {
    suspend fun create(showtimeId: Long, seatIds: List<Long>): ApiResult<CreatedBooking> =
        safeApiCall { api.createBooking(CreateBookingRequest(showtimeId, seatIds)).toDomain() }.also {
            // Remember it at once: if the app is killed during payment the customer can pick the order up again.
            if (it is ApiResult.Ok) settings.setPendingBooking(it.data.bookingId, it.data.holdExpiresAt?.toEpochMilli() ?: 0)
        }

    suspend fun get(id: Long): ApiResult<Booking> = safeApiCall { api.booking(id).toDomain() }

    suspend fun cancel(id: Long): ApiResult<Unit> = safeApiCall { api.cancelBooking(id) }.also {
        if (it is ApiResult.Ok) settings.clearPendingBooking()
    }

    /** Called once an order has reached an end state (paid, expired, cancelled, refunded). */
    suspend fun forgetPending() = settings.clearPendingBooking()

    /** Replaces the snacks, the voucher and the use of points on a pending booking (the whole desired state). */
    suspend fun updateExtras(id: Long, combos: Map<Long, Int>, voucherCode: String?, usePoints: Boolean): ApiResult<Extras> =
        safeApiCall {
            api.updateExtras(id, BookingExtrasRequest(
                combos = combos.filterValues { it > 0 }.map { ComboLineRequest(it.key, it.value) },
                voucherCode = voucherCode?.trim()?.takeIf { it.isNotEmpty() },
                usePoints = usePoints,
            )).toDomain()
        }

    suspend fun history(page: Int = 0): ApiResult<List<BookingSummary>> = safeApiCall { api.bookings(page).items.map { it.toDomain() } }

    /** The VNPay URL to open in a Custom Tab. Never decide a payment from what happens in the tab: ask [get]. */
    private val lastProvider = java.util.concurrent.ConcurrentHashMap<Long, PaymentProvider>()

    /** The method last used for an order (in memory only): the "payment failed" page names it and retries with it. */
    fun lastProvider(bookingId: Long): PaymentProvider = lastProvider[bookingId] ?: PaymentProvider.VNPAY

    suspend fun paymentUrl(bookingId: Long, provider: PaymentProvider = PaymentProvider.VNPAY): ApiResult<String> =
        safeApiCall { api.createPaymentUrl(PaymentUrlRequest(bookingId, provider.code.takeIf { provider != PaymentProvider.VNPAY })).payUrl }.also { if (it is ApiResult.Ok) lastProvider[bookingId] = provider }

    /** The methods the server has switched on; an unknown code is ignored so an older app survives a newer server. */
    suspend fun paymentMethods(): ApiResult<List<PaymentProvider>> =
        safeApiCall { api.paymentMethods().filter { it.enabled }.mapNotNull { PaymentProvider.parse(it.code) } }
}
