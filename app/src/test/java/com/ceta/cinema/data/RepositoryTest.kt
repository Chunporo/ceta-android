package com.ceta.cinema.data

import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.SeatRepository
import com.ceta.cinema.domain.model.BookingStatus
import com.ceta.cinema.domain.model.SeatStatus
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class RepositoryTest {
    private lateinit var server: MockWebServer

    @Before fun start() { server = MockWebServer().also { it.start() } }
    @After fun stop() { server.shutdown() }

    private fun tokens() = FakeTokenStore("access-1", "refresh-1")

    @Test fun seats_200_maps_status_price_and_unknown_values() = runTest {
        server.enqueue(json(fixture("seats.json")))
        val result = SeatRepository(apiFor(server, tokens())).seats(5)

        val seats = (result as ApiResult.Ok).data
        assertEquals(listOf(SeatStatus.AVAILABLE, SeatStatus.HELD, SeatStatus.SOLD, SeatStatus.INACTIVE, SeatStatus.INACTIVE), seats.map { it.status })
        assertEquals(90_000L, seats[0].price)
        assertTrue(seats[0].selectable)
        assertFalse("an unknown status must never be selectable", seats[4].selectable)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/showtimes/5/seats", recorded.path)
        assertEquals("Bearer access-1", recorded.getHeader("Authorization"))
    }

    @Test fun createBooking_409_exposes_conflicting_seat_ids() = runTest {
        server.enqueue(json(fixture("conflict_409.json"), code = 409))
        val result = BookingRepository(apiFor(server, tokens()), com.ceta.cinema.data.FakeSettingsStore()).create(5, listOf(7, 8, 9))

        val error = (result as ApiResult.Err).error
        assertEquals("SEAT_CONFLICT", error.code)
        assertEquals(409, error.httpStatus)
        assertEquals(listOf(7L, 9L), error.conflictSeatIds)
    }

    @Test fun booking_200_maps_hold_and_status() = runTest {
        server.enqueue(json(fixture("booking_pending.json")))
        val booking = (BookingRepository(apiFor(server, tokens()), com.ceta.cinema.data.FakeSettingsStore()).get(42) as ApiResult.Ok).data

        assertEquals(BookingStatus.PENDING, booking.status)
        assertEquals(Instant.parse("2026-11-17T08:10:00Z"), booking.holdExpiresAt)
        assertEquals(540L, booking.secondsRemaining)
        assertEquals(180_000L, booking.totalAmount)
        assertEquals(listOf("F7", "F8"), booking.seats.map { it.label })
    }

    @Test fun cancel_204_is_ok() = runTest {
        server.enqueue(okhttp3.mockwebserver.MockResponse().setResponseCode(204))
        assertTrue(BookingRepository(apiFor(server, tokens()), com.ceta.cinema.data.FakeSettingsStore()).cancel(42) is ApiResult.Ok)
    }

    @Test fun unreachable_server_is_a_network_error() = runTest {
        val api = apiFor(server, tokens())
        server.shutdown()
        val error = (SeatRepository(api).seats(5) as ApiResult.Err).error
        assertEquals("NETWORK", error.code)
    }

    @Test fun error_without_json_body_still_maps() = runTest {
        server.enqueue(okhttp3.mockwebserver.MockResponse().setResponseCode(502).setBody("<html>bad gateway</html>"))
        val error = (BookingRepository(apiFor(server, tokens()), com.ceta.cinema.data.FakeSettingsStore()).get(1) as ApiResult.Err).error
        assertEquals("UNKNOWN", error.code)
        assertEquals(502, error.httpStatus)
    }
}

class PendingBookingTest {
    private val server = MockWebServer()

    @Test fun aCreatedBookingIsRememberedAndForgottenWhenCancelled() = runTest {
        server.start()
        val settings = FakeSettingsStore()
        val repo = BookingRepository(apiFor(server, FakeTokenStore("a", "r")), settings)
        server.enqueue(json("""{"bookingId":42,"holdExpiresAt":"2026-11-18T12:00:00Z","totalAmount":170000,"seats":[]}""", 201))

        repo.create(5, listOf(1, 2))

        assertEquals(42L, settings.current.pendingBookingId)
        assertEquals(Instant.parse("2026-11-18T12:00:00Z").toEpochMilli(), settings.current.pendingHoldUntilMillis)

        server.enqueue(okhttp3.mockwebserver.MockResponse().setResponseCode(204))
        repo.cancel(42)
        assertNull(settings.current.pendingBookingId)
        server.shutdown()
    }

    @Test fun aRefusedBookingLeavesNothingPending() = runTest {
        server.start()
        val settings = FakeSettingsStore()
        server.enqueue(json("""{"code":"SEAT_CONFLICT","message":"x","details":[{"seatId":1}]}""", 409))
        BookingRepository(apiFor(server, FakeTokenStore("a", "r")), settings).create(5, listOf(1))
        assertNull(settings.current.pendingBookingId)
        server.shutdown()
    }
}
