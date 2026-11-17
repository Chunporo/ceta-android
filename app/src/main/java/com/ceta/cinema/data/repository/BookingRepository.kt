package com.ceta.cinema.data.repository

import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.safeApiCall
import com.ceta.cinema.data.local.prefs.SettingsStore
import com.ceta.cinema.data.remote.api.CetaApi
import com.ceta.cinema.data.remote.dto.BookingExtrasRequest
import com.ceta.cinema.data.remote.dto.ComboLineRequest
import com.ceta.cinema.data.remote.dto.CreateBookingRequest
import com.ceta.cinema.data.remote.dto.PaymentUrlRequest
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
    suspend fun paymentUrl(bookingId: Long): ApiResult<String> = safeApiCall { api.createPaymentUrl(PaymentUrlRequest(bookingId)).payUrl }
}
