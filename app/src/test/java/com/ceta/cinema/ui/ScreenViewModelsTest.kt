package com.ceta.cinema.ui

import androidx.lifecycle.SavedStateHandle
import com.ceta.cinema.auth.FakeFirebaseAuth
import com.ceta.cinema.auth.FakePendingEmail
import com.ceta.cinema.core.OfflineFlag
import com.ceta.cinema.core.SessionEvents
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.*
import com.ceta.cinema.data.local.prefs.AppSettings
import com.ceta.cinema.data.repository.AccountRepository
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.ui.screen.home.HomeViewModel
import com.ceta.cinema.ui.screen.movie.MovieDetailViewModel
import com.ceta.cinema.data.repository.MockTrendingRepository
import com.ceta.cinema.ui.screen.search.SearchViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenViewModelsTest {
    private lateinit var server: MockWebServer
    private val settings = FakeSettingsStore(AppSettings(displayName = "Minh"))
    private val tokens = FakeTokenStore("access", "refresh")
    private val offline = OfflineFlag()
    private val movieId = "11111111-1111-1111-1111-111111111111"
    private val requests = java.util.concurrent.CopyOnWriteArrayList<String>()
    private val answers = mutableMapOf<String, () -> MockResponse>()

    @Before fun start() {
        server = MockWebServer().also { s ->
            s.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests += "${request.method} ${request.path}"
                    val key = answers.keys.firstOrNull { request.path!!.startsWith(it.substringAfter(' ')) && request.method == it.substringBefore(' ') }
                    return answers[key]?.invoke() ?: MockResponse().setResponseCode(404).setBody("""{"code":"NOT_FOUND"}""")
                }
            }
            s.start()
        }
    }
    @After fun stop() { server.shutdown() } // Main is left set: a ViewModel from an earlier test may still have a coroutine in flight

    private fun TestScope.setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    }

    private fun api() = apiFor(server, tokens)
    private fun session(signedIn: Boolean): SessionRepository {
        if (!signedIn) tokens.accessToken = null
        return SessionRepository(api(), tokens, FakeFirebaseAuth(), FakePendingEmail(), settings)
    }
    private fun eventually(timeoutMs: Long = 3_000, check: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!check() && System.currentTimeMillis() < end) Thread.sleep(10)
        assertTrue("condition not met within ${timeoutMs}ms", check())
    }
    private fun ok(body: String) = { json(body) }

    // ---- Home

    @Test fun homeSplitsNowShowingFromComingSoonAndKeepsTheFirstFiveAsFeatured() = runTest {
        setup()
        val movies = (1..6).joinToString(",", "[", "]") { """{"id":"m$it","title":"Phim $it","status":"NOW_SHOWING","cast":[],"formats":[],"genres":[]}""" }
        answers["GET /api/v1/movies?status=NOW_SHOWING"] = ok(movies)
        answers["GET /api/v1/movies?status=COMING_SOON"] = ok("""[{"id":"s1","title":"Soon","status":"COMING_SOON"}]""")
        answers["GET /api/v1/promotions"] = ok((1..6).joinToString(",", "[", "]") { """{"id":$it,"code":"P$it","title":"Offer $it","kind":"COUNTER_PERK"}""" })

        val catalog = CatalogRepository(api(), offline)
        val vm = HomeViewModel(catalog, AccountRepository(api()), session(false), settings, offline, java.time.Clock.systemUTC())

        eventually { vm.state.value is UiState.Content }
        val data = (vm.state.value as UiState.Content).data
        assertEquals(6, data.nowShowing.size)
        assertEquals(5, data.featured.size)
        assertEquals(listOf("Soon"), data.comingSoon.map { it.title })
        assertEquals("offers on the home page are capped", HomeViewModel.OFFERS_ON_HOME, data.offers.size)
        assertEquals("Minh", vm.displayName.value)
    }

    @Test fun homeStillWorksWhenOnlyTheOffersFail() = runTest {
        setup()
        answers["GET /api/v1/movies?status=NOW_SHOWING"] = ok("""[{"id":"m1","title":"Phim"}]""")
        answers["GET /api/v1/movies?status=COMING_SOON"] = ok("[]")
        // promotions: 404 from the default answer
        val vm = HomeViewModel(CatalogRepository(api(), offline), AccountRepository(api()), session(false), settings, offline, java.time.Clock.systemUTC())
        eventually { vm.state.value is UiState.Content }
        assertTrue((vm.state.value as UiState.Content).data.offers.isEmpty())
    }

    @Test fun homeShowsAnErrorWhenTheMovieListFails() = runTest {
        setup()
        val vm = HomeViewModel(CatalogRepository(api(), offline), AccountRepository(api()), session(false), settings, offline, java.time.Clock.systemUTC())
        eventually { vm.state.value is UiState.Error }
    }

    @Test fun aGuestNeverAsksForUnreadNotifications() = runTest {
        setup()
        answers["GET /api/v1/movies?status=NOW_SHOWING"] = ok("[]")
        answers["GET /api/v1/movies?status=COMING_SOON"] = ok("[]")
        answers["GET /api/v1/promotions"] = ok("[]")
        val s = session(false); s.restore()
        val vm = HomeViewModel(CatalogRepository(api(), offline), AccountRepository(api()), s, settings, offline, java.time.Clock.systemUTC())
        eventually { vm.state.value is UiState.Content }
        Thread.sleep(100)
        assertTrue(requests.none { it.contains("unread-count") })
        assertEquals(0L, vm.unread.value)
    }

    @Test fun anUnpaidOrderWithSeatsStillHeldIsOfferedOnHomeAndAnOldOneIsNot() = runTest {
        setup()
        answers["GET /api/v1/movies?status=NOW_SHOWING"] = ok("[]")
        answers["GET /api/v1/movies?status=COMING_SOON"] = ok("[]")
        answers["GET /api/v1/promotions"] = ok("[]")
        settings.setPendingBooking(42, System.currentTimeMillis() + 300_000)
        val vm = HomeViewModel(CatalogRepository(api(), offline), AccountRepository(api()), session(false), settings, offline, java.time.Clock.systemUTC())
        eventually { vm.pendingOrder.value == 42L }

        settings.setPendingBooking(43, System.currentTimeMillis() - 1_000)
        eventually { vm.pendingOrder.value == null }
        settings.setPendingBooking(44, System.currentTimeMillis() + 300_000)
        settings.clearPendingBooking()
        eventually { vm.pendingOrder.value == null }
    }

    @Test fun aSignedInCustomerSeesTheUnreadBadge() = runTest {
        setup()
        answers["GET /api/v1/movies?status=NOW_SHOWING"] = ok("[]")
        answers["GET /api/v1/movies?status=COMING_SOON"] = ok("[]")
        answers["GET /api/v1/promotions"] = ok("[]")
        answers["GET /api/v1/notifications/unread-count"] = ok("""{"unread":3}""")
        val s = session(true); s.restore()
        val vm = HomeViewModel(CatalogRepository(api(), offline), AccountRepository(api()), s, settings, offline, java.time.Clock.systemUTC())
        eventually { vm.unread.value == 3L }
    }

    // ---- Search

    private val searchMovies = """[
        {"id":"m1","title":"Mùa Hè Rực Lửa","status":"NOW_SHOWING","ratingAverage":8.2,"ratingCount":30,"genres":[{"id":1,"name":"Hành động","slug":"x"}],"director":"Lê Văn Đạo","cast":["Thu Trang","Tiến Luật"],"formats":[]},
        {"id":"m2","title":"Dune","status":"NOW_SHOWING","genres":[{"id":1,"name":"Khoa học viễn tưởng","slug":"x"}],"cast":["Timothée Chalamet"],"formats":[]},
        {"id":"m3","title":"Căn Nhà Số 9","status":"COMING_SOON","releaseDate":"2026-12-05","genres":[{"id":1,"name":"Kinh dị","slug":"x"}],"cast":[],"formats":[]}]"""
    private val searchCinemas = """[{"id":1,"name":"CETA Landmark","city":"HCM","address":"208 Nguyễn Hữu Cảnh, Bình Thạnh","imax":true},
        {"id":2,"name":"CETA Tây Hồ","city":"HN","address":"Xuân La, Tây Hồ","imax":false}]"""

    private fun searchVm(): SearchViewModel {
        answers["GET /api/v1/movies"] = ok(searchMovies)
        answers["GET /api/v1/cinemas"] = ok(searchCinemas)
        answers["GET /api/v1/genres"] = ok("""[{"id":1,"name":"Hành động","slug":"hanh-dong"},{"id":2,"name":"Kinh dị","slug":"kinh-di"}]""")
        answers["GET /api/v1/showtimes"] = ok("[]")
        val vm = SearchViewModel(CatalogRepository(api(), offline), settings, MockTrendingRepository(CatalogRepository(api(), offline)), java.time.Clock.systemUTC())
        eventually { !vm.state.value.loading }
        return vm
    }

    @Test fun theIdleScreenHasGenreTilesWithCountsAndTrendingMovies() = runTest {
        setup()
        val vm = searchVm()
        val s = vm.state.value
        assertEquals(listOf("Hành động" to 1, "Kinh dị" to 1), s.genres.map { it.genre.name to it.count })
        assertEquals("only films in cinemas are trending", setOf("Mùa Hè Rực Lửa", "Dune"), s.trending.map { it.movie.title }.toSet())
        assertTrue(s.idle)
    }

    @Test fun typingWaitsForAPauseThenFiltersLocallyIgnoringAccents() = runTest {
        setup()
        val vm = searchVm()
        val before = requests.size
        vm.onQuery("m"); vm.onQuery("mua"); vm.onQuery("mua he")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS - 1)
        assertNull("nothing is searched before the pause", vm.state.value.results)
        advanceTimeBy(2)
        eventually { vm.state.value.results != null }
        assertEquals(listOf("Mùa Hè Rực Lửa"), vm.state.value.results!!.movies.map { it.movie.title })
        assertTrue("search does not hit the network for the match", requests.drop(before).none { it.contains("q=") })
    }

    @Test fun aPersonMatchNamesThePersonAndACinemaMatchesByNameOrAddress() = runTest {
        setup()
        val vm = searchVm()
        vm.pickRecent("thu trang")
        eventually { vm.state.value.results != null }
        assertEquals("Thu Trang", vm.state.value.results!!.movies.single().person)

        vm.pickRecent("tay ho")
        eventually { vm.state.value.results?.cinemas?.isNotEmpty() == true }
        assertEquals(listOf("CETA Tây Hồ"), vm.state.value.results!!.cinemas.map { it.name })
    }

    @Test fun clearingTheBoxGoesBackToTheIdleScreen() = runTest {
        setup()
        val vm = searchVm()
        vm.onQuery("dune")
        vm.onQuery("")
        advanceTimeBy(1_000)
        assertTrue(vm.state.value.idle)
        assertNull(vm.state.value.results)
    }

    @Test fun pickingAGenreSearchesByItsNameAndKeepsItInTheBox() = runTest {
        setup()
        val vm = searchVm()
        vm.pickGenre(vm.state.value.genres.first().genre)
        eventually { vm.state.value.results != null }
        assertEquals("Hành động", vm.state.value.query)
        assertEquals(listOf("Mùa Hè Rực Lửa"), vm.state.value.results!!.movies.map { it.movie.title })
    }

    @Test fun onlyWhatTheCustomerActsOnIsRememberedAndEachChipCanBeRemoved() = runTest {
        setup()
        val vm = searchVm()
        vm.onQuery("dune")
        assertTrue("typing alone is not remembered", settings.current.recentSearches.isEmpty())
        vm.remember()
        eventually { vm.state.value.recents == listOf("dune") }
        vm.onQuery("Mùa hè"); vm.remember()
        eventually { vm.state.value.recents == listOf("Mùa hè", "dune") }
        vm.removeRecent("DUNE")
        eventually { vm.state.value.recents == listOf("Mùa hè") }
        vm.clearRecents()
        eventually { vm.state.value.recents.isEmpty() }
    }

    @Test fun aCatalogThatCannotBeLoadedShowsAnErrorNotAnEmptyResult() = runTest {
        setup()
        answers["GET /api/v1/genres"] = ok("[]")
        val vm = SearchViewModel(CatalogRepository(api(), offline), settings, MockTrendingRepository(CatalogRepository(api(), offline)), java.time.Clock.systemUTC())
        eventually { vm.state.value.error != null }
        assertNull(vm.state.value.results)
    }

    // ---- Movie detail

    private fun movieBody(rating: String = "8.6", count: Int = 10) =
        """{"id":"$movieId","title":"Dune","status":"NOW_SHOWING","ratingAverage":$rating,"ratingCount":$count,"fromPrice":85000,"cast":[],"formats":[],"genres":[]}"""

    private fun detail(signedIn: Boolean): MovieDetailViewModel {
        val s = session(signedIn); runBlocking { s.restore() }
        return MovieDetailViewModel(SavedStateHandle(mapOf("movieId" to movieId)), CatalogRepository(api(), offline), AccountRepository(api()), s)
    }
    private fun content(vm: MovieDetailViewModel) = (vm.state.value as UiState.Content).data
    private fun <T> runBlocking(block: suspend () -> T) = kotlinx.coroutines.runBlocking { block() }

    @Test fun theDetailLoadsMovieReviewsOwnReviewAndReminder() = runTest {
        setup()
        answers["GET /api/v1/movies/$movieId/reviews"] = ok("""{"items":[{"id":1,"rating":9,"comment":"Hay","author":"Ng***","mine":true}],"page":0,"size":20,"totalItems":1,"totalPages":1}""")
        answers["GET /api/v1/movies/$movieId/review"] = ok("""{"id":1,"rating":9,"comment":"Hay","author":"Ng***","mine":true}""")
        answers["GET /api/v1/reminders"] = ok("""[{"id":"$movieId","title":"Dune"}]""")
        answers["GET /api/v1/movies/$movieId"] = ok(movieBody())
        val vm = detail(signedIn = true)

        eventually { vm.state.value is UiState.Content }
        val ui = content(vm)
        assertEquals(85_000L, ui.movie.fromPrice)
        assertEquals(1, ui.reviews.size)
        assertEquals(9, ui.myReview?.rating)
        assertTrue(ui.reminderOn)
    }

    @Test fun aGuestSeesTheMovieWithoutAskingForPrivateData() = runTest {
        setup()
        answers["GET /api/v1/movies/$movieId/reviews"] = ok("""{"items":[],"page":0,"size":20,"totalItems":0,"totalPages":0}""")
        answers["GET /api/v1/movies/$movieId"] = ok(movieBody())
        val vm = detail(signedIn = false)
        eventually { vm.state.value is UiState.Content }
        assertNull(content(vm).myReview)
        assertTrue(requests.none { it.endsWith("/review") || it.contains("reminders") })
    }

    @Test fun aGuestWhoTapsRemindMeIsSentToLogin() = runTest {
        setup()
        answers["GET /api/v1/movies/$movieId/reviews"] = ok("""{"items":[],"page":0,"size":20,"totalItems":0,"totalPages":0}""")
        answers["GET /api/v1/movies/$movieId"] = ok(movieBody())
        val vm = detail(signedIn = false)
        eventually { vm.state.value is UiState.Content }

        vm.toggleReminder()
        assertTrue(content(vm).needsLogin)
        assertTrue(requests.none { it.startsWith("PUT") })
        vm.consumeProblem()
        assertFalse(content(vm).needsLogin)
    }

    @Test fun aGuestAsksForTheReviewFormAndIsSentToLoginInstead() = runTest {
        setup()
        answers["GET /api/v1/movies/$movieId/reviews"] = ok("""{"items":[],"page":0,"size":20,"totalItems":0,"totalPages":0}""")
        answers["GET /api/v1/movies/$movieId"] = ok(movieBody())
        val vm = detail(signedIn = false)
        eventually { vm.state.value is UiState.Content }
        assertFalse(content(vm).signedIn)

        assertFalse("no form for a guest", vm.requestReview())
        assertTrue(content(vm).needsLogin)
    }

    @Test fun aSignedInCustomerGetsTheReviewForm() = runTest {
        setup()
        answers["GET /api/v1/movies/$movieId/reviews"] = ok("""{"items":[],"page":0,"size":20,"totalItems":0,"totalPages":0}""")
        answers["GET /api/v1/movies/$movieId/review"] = { MockResponse().setResponseCode(404).setBody("""{"code":"REVIEW_NOT_FOUND"}""") }
        answers["GET /api/v1/reminders"] = ok("[]")
        answers["GET /api/v1/movies/$movieId"] = ok(movieBody())
        val vm = detail(signedIn = true)
        eventually { vm.state.value is UiState.Content }
        assertTrue(content(vm).signedIn)
        assertTrue(vm.requestReview())
        assertFalse(content(vm).needsLogin)
    }

    @Test fun remindMeTogglesAndAnAlreadyOnSaleMovieExplainsWhy() = runTest {
        setup()
        answers["GET /api/v1/movies/$movieId/reviews"] = ok("""{"items":[],"page":0,"size":20,"totalItems":0,"totalPages":0}""")
        answers["GET /api/v1/movies/$movieId/review"] = { MockResponse().setResponseCode(404).setBody("""{"code":"REVIEW_NOT_FOUND"}""") }
        answers["GET /api/v1/reminders"] = ok("[]")
        answers["GET /api/v1/movies/$movieId"] = ok(movieBody())
        answers["PUT /api/v1/movies/$movieId/reminder"] = { MockResponse().setResponseCode(204) }
        answers["DELETE /api/v1/movies/$movieId/reminder"] = { MockResponse().setResponseCode(204) }
        val vm = detail(signedIn = true)
        eventually { vm.state.value is UiState.Content }
        assertFalse(content(vm).reminderOn)

        vm.toggleReminder()
        eventually { content(vm).reminderOn }
        vm.toggleReminder()
        eventually { !content(vm).reminderOn }

        answers["PUT /api/v1/movies/$movieId/reminder"] = { MockResponse().setResponseCode(409).setBody("""{"code":"ALREADY_ON_SALE"}""") }
        vm.toggleReminder()
        eventually { content(vm).problem?.code == "ALREADY_ON_SALE" }
        assertFalse(content(vm).reminderOn)
    }

    @Test fun writingAReviewRefreshesTheAverageAndAReviewByNonViewersIsExplained() = runTest {
        setup()
        answers["GET /api/v1/movies/$movieId/reviews"] = ok("""{"items":[],"page":0,"size":20,"totalItems":0,"totalPages":0}""")
        answers["GET /api/v1/movies/$movieId/review"] = { MockResponse().setResponseCode(404).setBody("""{"code":"REVIEW_NOT_FOUND"}""") }
        answers["GET /api/v1/reminders"] = ok("[]")
        answers["GET /api/v1/movies/$movieId"] = ok(movieBody("8.6", 10))
        answers["PUT /api/v1/movies/$movieId/review"] = { MockResponse().setResponseCode(403).setBody("""{"code":"REVIEW_NOT_ALLOWED"}""") }
        val vm = detail(signedIn = true)
        eventually { vm.state.value is UiState.Content }

        vm.saveReview(8, "Hay")
        eventually { content(vm).problem?.code == "REVIEW_NOT_ALLOWED" }
        assertNull(content(vm).myReview)
        vm.consumeProblem()

        answers["PUT /api/v1/movies/$movieId/review"] = ok("""{"id":7,"rating":8,"comment":"Hay","author":"Ng***","mine":true}""")
        answers["GET /api/v1/movies/$movieId"] = ok(movieBody("8.5", 11))
        vm.saveReview(8, "Hay")
        eventually { content(vm).myReview?.rating == 8 }
        eventually { content(vm).movie.ratingCount == 11L }
    }

    @Test fun deletingTheReviewClearsIt() = runTest {
        setup()
        answers["GET /api/v1/movies/$movieId/reviews"] = ok("""{"items":[],"page":0,"size":20,"totalItems":0,"totalPages":0}""")
        answers["GET /api/v1/movies/$movieId/review"] = ok("""{"id":1,"rating":9,"author":"Ng***","mine":true}""")
        answers["GET /api/v1/reminders"] = ok("[]")
        answers["GET /api/v1/movies/$movieId"] = ok(movieBody())
        answers["DELETE /api/v1/movies/$movieId/review"] = { MockResponse().setResponseCode(204) }
        val vm = detail(signedIn = true)
        eventually { vm.state.value is UiState.Content && content(vm).myReview != null }

        vm.deleteReview()

        eventually { content(vm).myReview == null && !content(vm).busy }
    }

    @Test fun aMovieThatCannotBeLoadedIsAnError() = runTest {
        setup()
        answers["GET /api/v1/movies/$movieId/reviews"] = ok("""{"items":[],"page":0,"size":20,"totalItems":0,"totalPages":0}""")
        val vm = detail(signedIn = false)
        eventually { vm.state.value is UiState.Error }
    }
}
