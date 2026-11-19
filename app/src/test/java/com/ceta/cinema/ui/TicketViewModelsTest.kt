package com.ceta.cinema.ui

import androidx.lifecycle.SavedStateHandle
import com.ceta.cinema.auth.FakeFirebaseAuth
import com.ceta.cinema.auth.FakePendingEmail
import com.ceta.cinema.core.OfflineFlag
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.*
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.data.repository.TicketRepository
import com.ceta.cinema.ui.screen.ticket.TicketDetailViewModel
import com.ceta.cinema.ui.screen.ticket.TicketsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class TicketViewModelsTest {
    private lateinit var server: MockWebServer
    private val offline = OfflineFlag()
    private val tokens = FakeTokenStore("access", "refresh")
    private val settings = FakeSettingsStore()
    private val clock = TestClock(Instant.parse("2026-11-18T07:00:00Z"))
    private val requests = CopyOnWriteArrayList<String>()
    private val answers = ConcurrentHashMap<String, () -> MockResponse>()

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
    @After fun stop() { server.shutdown() } // Main is left set: a ViewModel from an earlier test may still have a coroutine in flight

    private fun TestScope.setup() { Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler)) }
    private fun api() = apiFor(server, tokens)
    private fun eventually(timeoutMs: Long = 3_000, check: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!check() && System.currentTimeMillis() < end) Thread.sleep(10)
        assertTrue("condition not met within ${timeoutMs}ms", check())
    }
    private fun session(signedIn: Boolean): SessionRepository {
        if (!signedIn) tokens.accessToken = null
        return SessionRepository(api(), tokens, FakeFirebaseAuth(), FakePendingEmail(), settings).also { runBlocking { it.restore() } }
    }
    private fun <T> runBlocking(block: suspend () -> T) = kotlinx.coroutines.runBlocking { block() }

    private fun ticketJson(id: Int, booking: Int, seat: String, start: String, end: String, used: Boolean = false) =
        """{"ticketId":$id,"bookingId":$booking,"code":"T$id","seatId":$id,"seatLabel":"$seat","price":85000,"showtime":{"showtimeId":3,"startTime":"$start","endTime":"$end","movieTitle":"Dune","roomName":"R1","cinemaName":"CETA Landmark","format":"IMAX"},"qrPayload":"CETA1.$id.x.y","used":$used}"""
    private fun page(vararg items: String) = """{"items":[${items.joinToString(",")}],"page":0,"size":50,"totalItems":${items.size},"totalPages":1}"""

    @Test fun theListSplitsUpcomingFromWatchedAndGroupsSeatsPerOrder() = runTest {
        setup()
        answers["GET /api/v1/tickets/my"] = {
            json(page(
                ticketJson(1, 5, "F7", "2026-11-19T12:00:00Z", "2026-11-19T14:00:00Z"), ticketJson(2, 5, "F8", "2026-11-19T12:00:00Z", "2026-11-19T14:00:00Z"),
                ticketJson(3, 6, "A1", "2026-11-10T12:00:00Z", "2026-11-10T14:00:00Z", used = true)))
        }
        val vm = TicketsViewModel(TicketRepository(api(), offline), session(true), offline, clock)
        eventually { vm.state.value is UiState.Content }
        val data = (vm.state.value as UiState.Content).data
        assertEquals(listOf(listOf("F7", "F8")), data.upcoming.map { it.seats })
        assertEquals(listOf(6L), data.watched.map { it.bookingId })
        assertTrue(data.watched.single().allUsed)
    }

    @Test fun aGuestNeverAsksForTickets() = runTest {
        setup()
        val vm = TicketsViewModel(TicketRepository(api(), offline), session(false), offline, clock)
        Thread.sleep(100)
        assertTrue(requests.isEmpty())
        assertEquals(false, vm.signedIn.value)
    }

    @Test fun aFailedLoadIsAnErrorThatCanBeRetried() = runTest {
        setup()
        val vm = TicketsViewModel(TicketRepository(api(), offline), session(true), offline, clock)
        eventually { vm.state.value is UiState.Error }
        answers["GET /api/v1/tickets/my"] = { json(page()) }
        vm.load(forceRefresh = true)
        eventually { vm.state.value is UiState.Content }
        assertTrue((vm.state.value as UiState.Content).data.upcoming.isEmpty())
    }

    @Test fun theDetailShowsOneQrPerSeatOfTheOrderAndItsSnacks() = runTest {
        setup()
        answers["GET /api/v1/tickets/my"] = { json(page(
            ticketJson(2, 5, "F8", "2026-11-19T12:00:00Z", "2026-11-19T14:00:00Z"), ticketJson(1, 5, "F7", "2026-11-19T12:00:00Z", "2026-11-19T14:00:00Z", used = true),
            ticketJson(3, 6, "A1", "2026-11-19T12:00:00Z", "2026-11-19T14:00:00Z"))) }
        answers["GET /api/v1/bookings/5"] = { json("""{"id":5,"status":"PAID","totalAmount":289000,"seats":[],"tickets":[],"comboAmount":119000,"combos":[{"comboId":2,"name":"Combo Đôi","quantity":1,"unitPrice":119000}],"refundRequired":false}""") }
        val vm = TicketDetailViewModel(SavedStateHandle(mapOf("bookingId" to 5L)), TicketRepository(api(), offline), BookingRepository(api(), settings))
        eventually { vm.state.value is UiState.Content }
        val ui = (vm.state.value as UiState.Content).data
        assertEquals(listOf("F7", "F8"), ui.tickets.map { it.seatLabel })
        assertEquals(listOf(true, false), ui.tickets.map { it.used })
        assertEquals("Combo Đôi", ui.combos.single().name)
    }

    @Test fun theQrOpensEvenIfTheOrderDetailCannotBeLoaded() = runTest {
        setup()
        answers["GET /api/v1/tickets/my"] = { json(page(ticketJson(1, 5, "F7", "2026-11-19T12:00:00Z", "2026-11-19T14:00:00Z"))) }
        val vm = TicketDetailViewModel(SavedStateHandle(mapOf("bookingId" to 5L)), TicketRepository(api(), offline), BookingRepository(api(), settings))
        eventually { vm.state.value is UiState.Content }
        assertTrue((vm.state.value as UiState.Content).data.combos.isEmpty())
    }

    @Test fun someoneElsesOrderIsNotFound() = runTest {
        setup()
        answers["GET /api/v1/tickets/my"] = { json(page(ticketJson(1, 5, "F7", "2026-11-19T12:00:00Z", "2026-11-19T14:00:00Z"))) }
        val vm = TicketDetailViewModel(SavedStateHandle(mapOf("bookingId" to 99L)), TicketRepository(api(), offline), BookingRepository(api(), settings))
        eventually { vm.state.value is UiState.Error }
        assertEquals("TICKET_NOT_FOUND", (vm.state.value as UiState.Error).error.code)
    }
}
