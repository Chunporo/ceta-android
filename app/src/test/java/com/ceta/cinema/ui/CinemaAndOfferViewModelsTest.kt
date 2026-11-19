package com.ceta.cinema.ui

import com.ceta.cinema.auth.FakeFirebaseAuth
import com.ceta.cinema.auth.FakePendingEmail
import com.ceta.cinema.core.OfflineFlag
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.*
import com.ceta.cinema.data.repository.AccountRepository
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.domain.model.LanguageMode
import com.ceta.cinema.domain.model.ShowFormat
import com.ceta.cinema.domain.model.Showtime
import com.ceta.cinema.domain.usecase.ShowtimeGrouping
import com.ceta.cinema.ui.screen.cinema.CinemasViewModel
import com.ceta.cinema.ui.screen.offers.OffersViewModel
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
class CinemaAndOfferViewModelsTest {
    private lateinit var server: MockWebServer
    private val offline = OfflineFlag()
    private val tokens = FakeTokenStore("access", "refresh")
    private val settings = FakeSettingsStore()
    private val clock = TestClock(Instant.parse("2026-11-18T05:00:00Z"))
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
    private fun ok(body: String) = { json(body) }
    private fun eventually(timeoutMs: Long = 3_000, check: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!check() && System.currentTimeMillis() < end) Thread.sleep(10)
        assertTrue("condition not met within ${timeoutMs}ms", check())
    }
    private fun session(signedIn: Boolean): SessionRepository {
        if (!signedIn) tokens.accessToken = null
        return SessionRepository(api(), tokens, FakeFirebaseAuth(), FakePendingEmail(), settings).also { kotlinx.coroutines.runBlocking { it.restore() } }
    }

    private val cinemasJson = """[{"id":1,"name":"CETA Landmark","city":"HCM","address":"208 NHC","latitude":10.79,"longitude":106.72,"imax":true},
        {"id":2,"name":"CETA Bà Triệu","city":"HN","address":"191 Bà Triệu","imax":false},{"id":3,"name":"CETA Nguyễn Du","city":"HCM","address":"116 Nguyễn Du","imax":false}]"""
    private fun show(id: Long, movie: String, mid: String, utc: String) =
        """{"id":$id,"movie":{"id":"$mid","title":"$movie"},"cinema":{"id":1,"name":"CETA Landmark"},"startTime":"$utc","basePrice":85000,"format":"TWO_D","status":"SCHEDULED"}"""

    @Test fun cinemasOpenOnHcmWithOnlyThatCitysCinemasSortedByName() = runTest {
        setup()
        answers["GET /api/v1/cinemas"] = ok(cinemasJson)
        val vm = CinemasViewModel(CatalogRepository(api(), offline), session(false), offline, clock)
        eventually { vm.state.value is UiState.Content }
        val ui = (vm.state.value as UiState.Content).data
        assertEquals("HCM", ui.city)
        assertEquals(listOf("HCM", "HN"), ui.cities)
        assertEquals(listOf("CETA Landmark", "CETA Nguyễn Du"), ui.visible.map { it.name })
    }

    @Test fun theFirstCinemaOfTheCityIsOpenAtOnceAndChangingCityOpensThatCitysFirst() = runTest {
        setup()
        answers["GET /api/v1/cinemas"] = ok(cinemasJson)
        answers["GET /api/v1/showtimes"] = ok("[]")
        val vm = CinemasViewModel(CatalogRepository(api(), offline), session(false), offline, clock)
        eventually { vm.state.value is UiState.Content && (vm.state.value as UiState.Content).data.open != null }
        assertEquals(1L, (vm.state.value as UiState.Content).data.open)       // CETA Landmark, first by name in HCM
        eventually { (vm.state.value as UiState.Content).data.shows[1L] is UiState.Content }
        vm.selectCity("HN")
        val ui = (vm.state.value as UiState.Content).data
        assertEquals("HN", ui.city)
        assertEquals(2L, ui.open)                                              // CETA Bà Triệu, the only one in HN
        assertEquals(listOf("CETA Bà Triệu"), ui.visible.map { it.name })
    }

    @Test fun openingACardLoadsTodaysShowsOnceAndReopeningUsesWhatWasLoaded() = runTest {
        setup()
        answers["GET /api/v1/cinemas"] = ok(cinemasJson)
        answers["GET /api/v1/showtimes"] = ok("[${show(10, "Dune", "m1", "2026-11-18T12:00:00Z")}]")
        val vm = CinemasViewModel(CatalogRepository(api(), offline), session(false), offline, clock)
        eventually { vm.state.value is UiState.Content }

        // The first cinema opens by itself, so its showings load without a tap.
        eventually { (vm.state.value as UiState.Content).data.shows[1L] is UiState.Content }
        val path = requests.last { it.contains("showtimes") }
        assertTrue(path, path.contains("cinemaId=1") && path.contains("date=2026-11-18"))
        assertEquals(1L, (vm.state.value as UiState.Content).data.open)
        vm.toggle(1)                                    // close
        assertNull((vm.state.value as UiState.Content).data.open)
        requests.clear()
        vm.toggle(1)                                    // reopen
        assertEquals(1L, (vm.state.value as UiState.Content).data.open)
        Thread.sleep(80)
        assertTrue("no second request for the same cinema and day", requests.none { it.contains("showtimes") })
    }

    @Test fun oneCinemasFailureDoesNotSpoilTheOthers() = runTest {
        setup()
        answers["GET /api/v1/cinemas"] = ok(cinemasJson)
        val vm = CinemasViewModel(CatalogRepository(api(), offline), session(false), offline, clock)
        eventually { vm.state.value is UiState.Content }
        vm.toggle(1)
        eventually { (vm.state.value as UiState.Content).data.shows[1L] is UiState.Error }
        assertEquals(1, (vm.state.value as UiState.Content).data.visible.count { it.id == 1L })
        answers["GET /api/v1/showtimes"] = ok("[]")
        vm.toggle(3)
        eventually { (vm.state.value as UiState.Content).data.shows[3L] is UiState.Content }
    }

    @Test fun noCinemasIsAnEmptyStateAndAFailureIsAnError() = runTest {
        setup()
        answers["GET /api/v1/cinemas"] = ok("[]")
        val empty = CinemasViewModel(CatalogRepository(api(), offline), session(false), offline, clock)
        eventually { empty.state.value is UiState.Empty }
        answers.clear()
        val failing = CinemasViewModel(CatalogRepository(api(), OfflineFlag()), session(false), OfflineFlag(), clock)
        eventually { failing.state.value is UiState.Error }
    }

    @Test fun showsAreGroupedByMovieWithEachMoviesTimesInOrder() {
        val now = Instant.parse("2026-11-18T05:00:00Z")
        val s = listOf(
            Showtime(1, "m2", "Zorro", null, 1, "C", "R", Instant.parse("2026-11-18T13:00:00Z"), null, 85_000, ShowFormat.TWO_D, LanguageMode.SUB, false),
            Showtime(2, "m1", "Dune", "p", 1, "C", "R", Instant.parse("2026-11-18T12:00:00Z"), null, 85_000, ShowFormat.TWO_D, LanguageMode.SUB, false),
            Showtime(3, "m1", "Dune", "p", 1, "C", "R", Instant.parse("2026-11-18T09:00:00Z"), null, 85_000, ShowFormat.IMAX, LanguageMode.DUB, false))
        val groups = ShowtimeGrouping.byMovie(s, now)
        assertEquals(listOf("Dune", "Zorro"), groups.map { it.movieTitle })
        assertEquals(listOf(3L, 2L), groups[0].chips.map { it.showtime.id })
        assertEquals("p", groups[0].posterUrl)
    }

    // ---- offers

    private val offersJson = """[{"id":1,"code":"CETA20","title":"New members","kind":"PERCENT_OFF","percentOff":20},{"id":4,"code":"HSSV49","title":"Student","kind":"COUNTER_PERK"}]"""
    private fun offers(signedIn: Boolean) = OffersViewModel(CatalogRepository(api(), offline), AccountRepository(api()), session(signedIn), offline)
    private fun ui(vm: OffersViewModel) = (vm.state.value as UiState.Content).data

    @Test fun offersShowWhichOnesAreSavedInTheWallet() = runTest {
        setup()
        answers["GET /api/v1/promotions"] = ok(offersJson)
        answers["GET /api/v1/vouchers"] = ok("""[{"promotion":{"id":1,"code":"CETA20","title":"x","kind":"PERCENT_OFF"},"savedAt":"2026-11-01T00:00:00Z"}]""")
        val vm = offers(true)
        eventually { vm.state.value is UiState.Content }
        assertEquals(setOf(1L), ui(vm).saved)
        assertEquals(2, ui(vm).offers.size)
    }

    @Test fun aGuestSeesTheOffersWithoutTouchingTheWalletAndIsSentToLoginToSave() = runTest {
        setup()
        answers["GET /api/v1/promotions"] = ok(offersJson)
        val vm = offers(false)
        eventually { vm.state.value is UiState.Content }
        assertTrue(requests.none { it.contains("vouchers") })
        vm.toggleSave(ui(vm).offers.first())
        assertTrue(ui(vm).needsLogin)
        assertTrue(requests.none { it.startsWith("PUT") })
        vm.consumeMessage()
        assertFalse(ui(vm).needsLogin)
    }

    @Test fun savingShowsAtOnceAndConfirmsAndUnsavingRemoves() = runTest {
        setup()
        answers["GET /api/v1/promotions"] = ok(offersJson)
        answers["GET /api/v1/vouchers"] = ok("[]")
        answers["PUT /api/v1/vouchers/1"] = ok("""{"promotion":{"id":1,"code":"CETA20","title":"x","kind":"PERCENT_OFF"},"savedAt":"2026-11-01T00:00:00Z"}""")
        answers["DELETE /api/v1/vouchers/1"] = { MockResponse().setResponseCode(204) }
        val vm = offers(true)
        eventually { vm.state.value is UiState.Content }
        val promo = ui(vm).offers.first()

        vm.toggleSave(promo)
        assertTrue("shown as saved immediately", 1L in ui(vm).saved)
        eventually { ui(vm).justSaved && 1L !in ui(vm).busy }

        vm.toggleSave(promo)
        eventually { 1L !in ui(vm).saved && 1L !in ui(vm).busy }
        assertFalse(ui(vm).justSaved)
    }

    @Test fun aSaveTheServerRefusesIsPutBack() = runTest {
        setup()
        answers["GET /api/v1/promotions"] = ok(offersJson)
        answers["GET /api/v1/vouchers"] = ok("[]")
        answers["PUT /api/v1/vouchers/1"] = { json("""{"code":"NOT_FOUND","message":"x","details":[]}""", 404) }
        val vm = offers(true)
        eventually { vm.state.value is UiState.Content }
        vm.toggleSave(ui(vm).offers.first())
        eventually { ui(vm).problem != null && 1L !in ui(vm).busy }
        assertFalse(1L in ui(vm).saved)
    }

    @Test fun aDoubleTapSendsOneRequest() = runTest {
        setup()
        answers["GET /api/v1/promotions"] = ok(offersJson)
        answers["GET /api/v1/vouchers"] = ok("[]")
        answers["PUT /api/v1/vouchers/1"] = { json("""{"promotion":{"id":1,"code":"CETA20","title":"x","kind":"PERCENT_OFF"}}""").setBodyDelay(200, java.util.concurrent.TimeUnit.MILLISECONDS) }
        val vm = offers(true)
        eventually { vm.state.value is UiState.Content }
        val promo = ui(vm).offers.first()
        vm.toggleSave(promo); vm.toggleSave(promo)
        eventually { 1L !in ui(vm).busy }
        assertEquals(1, requests.count { it.startsWith("PUT") })
    }

    @Test fun offersThatCannotLoadAreAnError() = runTest {
        setup()
        val vm = offers(false)
        eventually { vm.state.value is UiState.Error }
    }
}
