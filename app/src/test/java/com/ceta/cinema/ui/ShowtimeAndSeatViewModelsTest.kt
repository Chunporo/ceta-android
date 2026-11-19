package com.ceta.cinema.ui

import androidx.lifecycle.SavedStateHandle
import com.ceta.cinema.core.OfflineFlag
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.*
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.data.repository.SeatRepository
import com.ceta.cinema.domain.model.ShowFormat
import com.ceta.cinema.ui.screen.seat.SeatMapViewModel
import com.ceta.cinema.ui.screen.seat.SeatMessage
import com.ceta.cinema.ui.screen.showtime.ShowtimePickerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ShowtimeAndSeatViewModelsTest {
    private lateinit var server: MockWebServer
    private val offline = OfflineFlag()
    private val tokens = FakeTokenStore("access", "refresh")
    private val requests = java.util.concurrent.CopyOnWriteArrayList<String>()
    private val answers = java.util.concurrent.ConcurrentHashMap<String, () -> MockResponse>()

    @Before fun start() {
        server = MockWebServer().also { s ->
            s.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests += "${request.method} ${request.path}"
                    val key = answers.keys.sortedByDescending { it.length }.firstOrNull { request.path!!.startsWith(it.substringAfter(' ')) && request.method == it.substringBefore(' ') }
                    return key?.let { answers[it] }?.invoke() ?: MockResponse().setResponseCode(404).setBody("""{"code":"NOT_FOUND"}""")
                }
            }
            s.start()
        }
    }
    // Main is not reset: a ViewModel from an earlier test may still have a coroutine in flight, and it must not find Main gone.
    @After fun stop() { server.shutdown() }

    private fun TestScope.setup() { Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler)) }
    private fun api() = apiFor(server, tokens)
    private fun ok(body: String) = { json(body) }
    private fun eventually(timeoutMs: Long = 3_000, check: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!check() && System.currentTimeMillis() < end) Thread.sleep(10)
        assertTrue("condition not met within ${timeoutMs}ms", check())
    }

    // ---- showtime picker

    private val fixedClock = Clock.fixed(Instant.parse("2026-11-18T05:00:00Z"), ZoneOffset.UTC)
    private val show = """[{"id":501,"movie":{"id":"m1","title":"Dune"},"cinema":{"id":1,"name":"CETA Landmark"},"startTime":"2026-11-18T12:30:00Z","basePrice":85000,"format":"IMAX","status":"SCHEDULED"}]"""

    private fun picker() = ShowtimePickerViewModel(SavedStateHandle(mapOf("movieId" to "m1")), CatalogRepository(api(), offline), fixedClock)

    @Test fun thePickerOpensOnTodayInTheCinemasTimeZoneAndLoadsIt() = runTest {
        setup()
        answers["GET /api/v1/showtimes"] = ok(show)
        val vm = picker()
        eventually { vm.state.value.shows is UiState.Content }
        assertEquals(LocalDate.of(2026, 11, 18), vm.state.value.date)
        assertEquals(7, vm.state.value.week.size)
        assertTrue(requests.single().contains("date=2026-11-18") && requests.single().contains("movieId=m1"))
    }

    @Test fun pickingAnotherDayLoadsThatDayAndFormatOrViewChangesDoNotReload() = runTest {
        setup()
        answers["GET /api/v1/showtimes"] = ok(show)
        val vm = picker()
        eventually { vm.state.value.shows is UiState.Content }
        requests.clear()

        vm.selectFormat(ShowFormat.IMAX)
        vm.toggleView()
        assertTrue(requests.isEmpty())
        assertFalse(vm.state.value.byCinema)
        assertEquals(ShowFormat.IMAX, vm.state.value.format)

        vm.selectDate(LocalDate.of(2026, 11, 20))
        eventually { requests.any { it.contains("date=2026-11-20") } }
        vm.selectDate(LocalDate.of(2026, 11, 20))
        assertEquals("choosing the same day again does nothing", 1, requests.size)
    }

    @Test fun aSlowAnswerForADayTheCustomerLeftIsIgnored() = runTest {
        setup()
        answers["GET /api/v1/showtimes?movieId=m1&date=2026-11-18"] = { json(show).setBodyDelay(400, java.util.concurrent.TimeUnit.MILLISECONDS) }
        answers["GET /api/v1/showtimes?movieId=m1&date=2026-11-19"] = ok("[]")
        val vm = picker()
        vm.selectDate(LocalDate.of(2026, 11, 19))
        eventually { (vm.state.value.shows as? UiState.Content)?.data?.isEmpty() == true }
        Thread.sleep(600)
        assertTrue("the old day's list must not appear on the new day", (vm.state.value.shows as UiState.Content).data.isEmpty())
        assertEquals(LocalDate.of(2026, 11, 19), vm.state.value.date)
    }

    @Test fun aFailedLoadShowsAnErrorAndCanBeRetried() = runTest {
        setup()
        val vm = picker()
        eventually { vm.state.value.shows is UiState.Error }
        answers["GET /api/v1/showtimes"] = ok(show)
        vm.load()
        eventually { vm.state.value.shows is UiState.Content }
    }

    // ---- seat map

    private fun seatsJson(taken: Set<Int> = emptySet(), myHold: Set<Int> = emptySet()) = (1..10).joinToString(",", "[", "]") { n ->
        val status = when (n) { in taken -> "SOLD"; in myHold -> "HELD_BY_ME"; else -> "AVAILABLE" }
        """{"seatId":$n,"label":"A$n","rowNumber":1,"columnNumber":$n,"type":"STANDARD","active":true,"status":"$status","price":85000}"""
    }
    private val showJson = """{"id":501,"movie":{"id":"m1","title":"Dune"},"cinema":{"id":1,"name":"CETA Landmark"},"startTime":"2026-11-18T12:30:00Z","basePrice":85000,"format":"IMAX","status":"SCHEDULED"}"""

    private fun seatMap(clock: Clock = Clock.systemUTC(), state: SavedStateHandle = SavedStateHandle(mapOf("showtimeId" to 501L))): SeatMapViewModel =
        SeatMapViewModel(state, SeatRepository(api()), CatalogRepository(api(), offline), BookingRepository(api(), FakeSettingsStore()), clock)

    /** A clock the test moves by hand. */
    private class Hand(var now: Instant = Instant.parse("2026-11-18T07:00:00Z")) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = now
        fun advanceMinutes(m: Long) { now = now.plusSeconds(m * 60) }
    }

    private fun ui(vm: SeatMapViewModel) = (vm.state.value as UiState.Content).data

    private fun seatApi(json: String = seatsJson()) {
        answers["GET /api/v1/showtimes/501/seats"] = ok(json)
        answers["GET /api/v1/showtimes/501"] = ok(showJson)
    }

    @Test fun theMapAndTheShowtimeLoadAndTheMapIsAskedEveryTime() = runTest {
        setup()
        seatApi()
        val vm = seatMap()
        eventually { vm.state.value is UiState.Content }
        assertEquals(10, ui(vm).seats.size)
        assertEquals("Dune", ui(vm).showtime?.movieTitle)
        vm.load(); vm.load()
        eventually { requests.count { it.endsWith("/seats") } == 3 }
    }

    @Test fun selectingSeatsAddsUpThePriceAndLimitsToEight() = runTest {
        setup()
        seatApi()
        val vm = seatMap()
        eventually { vm.state.value is UiState.Content }

        (1..8).forEach { vm.onSeat(it.toLong()) }
        assertEquals(8, ui(vm).selected.size)
        assertEquals(8 * 85_000L, ui(vm).total)

        vm.onSeat(9)
        assertEquals(8, ui(vm).selected.size)
        assertEquals(SeatMessage.MaxSeats, ui(vm).message)
        vm.consumeMessage()
        assertNull(ui(vm).message)
    }

    @Test fun soldAndHeldSeatsCannotBeChosen() = runTest {
        setup()
        seatApi(seatsJson(taken = setOf(2), myHold = setOf(3)))
        val vm = seatMap()
        eventually { vm.state.value is UiState.Content }
        vm.onSeat(2); vm.onSeat(3)
        assertTrue(ui(vm).selected.isEmpty())
        assertNull(ui(vm).message)
    }

    @Test fun theSeatCountdownStartsAtTheFirstSeatWarnsOnceAtTwoMinutesAndClearsTheSeatsAtTenMinutes() = runTest {
        setup()
        seatApi()
        val clock = Hand()
        val vm = seatMap(clock)
        eventually { vm.state.value is UiState.Content }
        assertNull("no countdown before a seat is chosen", ui(vm).holdSecondsLeft)

        vm.onSeat(1)
        assertEquals(600L, ui(vm).holdSecondsLeft)
        clock.advanceMinutes(3); vm.onSeat(2)                       // a second seat does not restart it
        vm.tick()
        assertEquals(420L, ui(vm).holdSecondsLeft)
        assertFalse(ui(vm).holdWarning)

        clock.advanceMinutes(5); vm.tick()                          // 2:00 left
        assertTrue(ui(vm).holdWarning)
        assertEquals(SeatMessage.HoldWarning, ui(vm).message)
        vm.consumeMessage(); vm.tick()
        assertNull("the warning is shown once", ui(vm).message)

        clock.advanceMinutes(2); vm.tick()
        assertTrue(ui(vm).selected.isEmpty())
        assertEquals(SeatMessage.HoldExpired, ui(vm).message)
        assertNull(ui(vm).holdSecondsLeft)
    }

    @Test fun unselectingEverySeatEndsTheCountdownAndTheNextSeatStartsANewOne() = runTest {
        setup()
        seatApi()
        val clock = Hand()
        val vm = seatMap(clock)
        eventually { vm.state.value is UiState.Content }
        vm.onSeat(1)
        clock.advanceMinutes(4)
        vm.onSeat(1)
        assertNull(ui(vm).holdSecondsLeft)
        vm.onSeat(2)
        assertEquals(600L, ui(vm).holdSecondsLeft)
    }

    @Test fun beingSentBackFromCheckoutAfterTheHoldRanOutClearsTheSeats() = runTest {
        setup()
        seatApi()
        val state = SavedStateHandle(mapOf("showtimeId" to 501L))
        val vm = seatMap(Hand(), state)
        eventually { vm.state.value is UiState.Content }
        vm.onSeat(1); vm.onSeat(2)
        state[SeatMapViewModel.HOLD_EXPIRED_KEY] = true
        vm.tick()                                   // the screen ticks once a second and picks the flag up
        assertEquals(SeatMessage.HoldExpired, ui(vm).message)
        assertNull(ui(vm).holdSecondsLeft)
        assertEquals("the flag is consumed", false, state.get<Boolean>(SeatMapViewModel.HOLD_EXPIRED_KEY))
    }

    @Test fun aRefreshDropsASeatSomeoneElseJustTookAndSaysSo() = runTest {
        setup()
        seatApi()
        val vm = seatMap()
        eventually { vm.state.value is UiState.Content }
        vm.onSeat(1); vm.onSeat(2)

        answers["GET /api/v1/showtimes/501/seats"] = ok(seatsJson(taken = setOf(2)))
        vm.refresh()

        eventually { ui(vm).selected == setOf(1L) }
        assertEquals(SeatMessage.SeatsTaken(listOf("A2")), ui(vm).message)
        assertEquals(85_000L, ui(vm).total)
    }

    @Test fun aFailedRefreshKeepsTheMapAndTheSelection() = runTest {
        setup()
        seatApi()
        val vm = seatMap()
        eventually { vm.state.value is UiState.Content }
        vm.onSeat(1)

        answers.remove("GET /api/v1/showtimes/501/seats")
        vm.refresh()
        Thread.sleep(150)

        assertEquals(setOf(1L), ui(vm).selected)
        assertEquals(10, ui(vm).seats.size)
    }

    @Test fun continuingCreatesOneBookingEvenOnADoubleTapAndAnnouncesIt() = runTest {
        setup()
        seatApi()
        answers["POST /api/v1/bookings"] = { json("""{"bookingId":42,"holdExpiresAt":"2026-11-18T12:00:00Z","totalAmount":170000,"seats":[]}""", 201).setBodyDelay(200, java.util.concurrent.TimeUnit.MILLISECONDS) }
        val vm = seatMap()
        eventually { vm.state.value is UiState.Content }
        vm.onSeat(1); vm.onSeat(2)

        vm.continueBooking()
        vm.continueBooking()

        assertEquals(42L, vm.booked.first())
        assertEquals(1, requests.count { it.startsWith("POST /api/v1/bookings") })
        assertFalse(ui(vm).submitting)
    }

    @Test fun continuingWithNothingSelectedDoesNothing() = runTest {
        setup()
        seatApi()
        val vm = seatMap()
        eventually { vm.state.value is UiState.Content }
        vm.continueBooking()
        assertTrue(requests.none { it.startsWith("POST") })
    }

    @Test fun aSeatConflictHighlightsTheOnesLostAndRefreshesTheMap() = runTest {
        setup()
        seatApi()
        answers["POST /api/v1/bookings"] = { json("""{"code":"SEAT_CONFLICT","message":"x","details":[{"seatId":2}]}""", 409) }
        val vm = seatMap()
        eventually { vm.state.value is UiState.Content }
        vm.onSeat(1); vm.onSeat(2)
        answers["GET /api/v1/showtimes/501/seats"] = ok(seatsJson(taken = setOf(2)))

        vm.continueBooking()

        eventually { ui(vm).conflicts == setOf(2L) }
        assertEquals(setOf(1L), ui(vm).selected)
        assertEquals("SEAT_CONFLICT", (ui(vm).message as SeatMessage.Failed).error.code)
        assertFalse(ui(vm).submitting)
        eventually { requests.count { it.endsWith("/seats") } >= 2 }
    }

    @Test fun anotherRefusalIsReportedAndKeepsTheSelection() = runTest {
        setup()
        seatApi()
        answers["POST /api/v1/bookings"] = { json("""{"code":"BOOKING_ALREADY_PENDING","message":"x","details":[]}""", 409) }
        val vm = seatMap()
        eventually { vm.state.value is UiState.Content }
        vm.onSeat(1)
        vm.continueBooking()
        eventually { ui(vm).message is SeatMessage.Failed }
        assertEquals("BOOKING_ALREADY_PENDING", (ui(vm).message as SeatMessage.Failed).error.code)
        assertEquals(setOf(1L), ui(vm).selected)
        assertTrue(ui(vm).conflicts.isEmpty())
    }

    @Test fun rechoosingAConflictingSeatClearsItsWarning() = runTest {
        setup()
        seatApi()
        answers["POST /api/v1/bookings"] = { json("""{"code":"SEAT_CONFLICT","message":"x","details":[{"seatId":2}]}""", 409) }
        val vm = seatMap()
        eventually { vm.state.value is UiState.Content }
        vm.onSeat(1); vm.onSeat(2)
        vm.continueBooking()
        eventually { ui(vm).conflicts == setOf(2L) }
        answers["GET /api/v1/showtimes/501/seats"] = ok(seatsJson()) // free again
        vm.refresh()
        eventually { ui(vm).conflicts.isEmpty() }
    }

    @Test fun aMapThatCannotBeLoadedIsAnError() = runTest {
        setup()
        val vm = seatMap()
        eventually { vm.state.value is UiState.Error }
    }
}
