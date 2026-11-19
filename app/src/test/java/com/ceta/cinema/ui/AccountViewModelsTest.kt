package com.ceta.cinema.ui

import com.ceta.cinema.auth.FakeFirebaseAuth
import com.ceta.cinema.auth.FakePendingEmail
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.*
import com.ceta.cinema.data.local.prefs.AppSettings
import com.ceta.cinema.data.repository.AccountRepository
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.domain.model.BookingStatus
import com.ceta.cinema.domain.model.Loyalty
import com.ceta.cinema.ui.screen.account.AccountViewModel
import com.ceta.cinema.ui.screen.account.HistoryViewModel
import com.ceta.cinema.ui.screen.account.NotificationsViewModel
import com.ceta.cinema.ui.screen.account.ProfileMessage
import com.ceta.cinema.ui.screen.account.ProfileViewModel
import com.ceta.cinema.ui.screen.account.RemindersViewModel
import com.ceta.cinema.ui.screen.account.VouchersViewModel
import com.ceta.cinema.ui.screen.account.initialsOf
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelsTest {
    private lateinit var server: MockWebServer
    private val tokens = FakeTokenStore("access", "refresh")
    private val settings = FakeSettingsStore(AppSettings(displayName = "Minh"))
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

    private val loyaltyJson = """{"balance":2450,"lifetimeEarned":2450,"tier":"MEMBER","nextTier":"VIP","pointsToNextTier":2550,"redeemBlockPoints":2000,"redeemBlockVnd":20000,"stats":{"ticketsWatched":12,"vouchers":3,"reviews":5}}"""

    @Test fun theAccountShowsNamePointsTierAndCounters() = runTest {
        setup()
        answers["GET /api/v1/me/loyalty"] = ok(loyaltyJson)
        val vm = AccountViewModel(AccountRepository(api()), session(true), settings)
        eventually { vm.loyalty.value is UiState.Content }
        val l = (vm.loyalty.value as UiState.Content).data
        assertEquals("Minh", vm.displayName.value)
        assertEquals(2450, l.balance)
        assertEquals(12, l.ticketsWatched)
        assertEquals(0.49f, l.progressToNextTier, 0.001f)
    }

    @Test fun aGuestNeverAsksForPoints() = runTest {
        setup()
        AccountViewModel(AccountRepository(api()), session(false), settings)
        Thread.sleep(100)
        assertTrue(requests.isEmpty())
    }

    @Test fun theTopTierIsAFullBarAndNeedsNothingMore() {
        val vip = Loyalty(9000, 9000, "VIP", null, 0, 2000, 20_000, 1, 1, 1)
        assertTrue(vip.isTopTier)
        assertEquals(1f, vip.progressToNextTier, 0f)
        val fresh = Loyalty(0, 0, "MEMBER", "VIP", 5000, 2000, 20_000, 0, 0, 0)
        assertEquals(0f, fresh.progressToNextTier, 0f)
        assertFalse(fresh.canRedeem)
    }

    @Test fun theHistoryListsOrdersWithTheirStatus() = runTest {
        setup()
        answers["GET /api/v1/bookings?page=0"] = ok("""{"items":[{"id":42,"status":"PAID","totalAmount":170000,"createdAt":"2026-11-17T01:00:00Z","startTime":"2026-11-18T12:00:00Z","movieTitle":"Dune","roomName":"R1","seatNames":["F7","F8"]},
            {"id":41,"status":"EXPIRED","totalAmount":85000,"movieTitle":"Dune","seatNames":["A1"]}],"page":0,"size":20,"totalItems":2,"totalPages":1}""")
        val vm = HistoryViewModel(BookingRepository(api(), settings))
        eventually { vm.state.value is UiState.Content }
        val list = (vm.state.value as UiState.Content).data
        assertEquals(listOf(BookingStatus.PAID, BookingStatus.EXPIRED), list.map { it.status })
        assertEquals(listOf("F7", "F8"), list[0].seatNames)
    }

    @Test fun anEmptyHistoryIsAnEmptyStateAndAFailureAnError() = runTest {
        setup()
        answers["GET /api/v1/bookings?page=0"] = ok("""{"items":[],"page":0,"size":20,"totalItems":0,"totalPages":0}""")
        val empty = HistoryViewModel(BookingRepository(api(), settings))
        eventually { empty.state.value is UiState.Empty }
        answers.clear()
        val failing = HistoryViewModel(BookingRepository(api(), settings))
        eventually { failing.state.value is UiState.Error }
    }

    private val notesJson = """{"items":[{"id":1,"type":"ORDER_PAID","title":"Đặt vé thành công","body":"b","bookingId":42,"read":false},
        {"id":2,"type":"MOVIE_ON_SALE","title":"Phim đã mở bán vé","body":"b","movieId":"m9","read":false},{"id":3,"type":"REFUND_DONE","title":"t","body":"b","bookingId":41,"read":true}],"page":0,"size":30,"totalItems":3,"totalPages":1}"""
    private fun notes() = NotificationsViewModel(AccountRepository(api()))
    private fun nu(vm: NotificationsViewModel) = (vm.state.value as UiState.Content).data

    @Test fun notificationsLoadWithTheirLinksAndAnUnreadCount() = runTest {
        setup()
        answers["GET /api/v1/notifications?page=0"] = ok(notesJson)
        val vm = notes()
        eventually { vm.state.value is UiState.Content }
        assertEquals(2, nu(vm).unread)
        assertEquals("m9", nu(vm).items[1].movieId)
        assertEquals(42L, nu(vm).items[0].bookingId)
    }

    @Test fun readingOneMarksItReadAtOnceAndAnAlreadyReadOneSendsNothing() = runTest {
        setup()
        answers["GET /api/v1/notifications?page=0"] = ok(notesJson)
        answers["PUT /api/v1/notifications/1/read"] = { MockResponse().setResponseCode(204) }
        val vm = notes()
        eventually { vm.state.value is UiState.Content }
        vm.markRead(1)
        assertEquals(1, nu(vm).unread)
        vm.markRead(3)
        Thread.sleep(80)
        assertEquals("only the unread one was sent", 1, requests.count { it.startsWith("PUT") })
    }

    @Test fun aRefusedMarkReadGoesBackToUnread() = runTest {
        setup()
        answers["GET /api/v1/notifications?page=0"] = ok(notesJson)
        val vm = notes()
        eventually { vm.state.value is UiState.Content }
        vm.markRead(1)
        eventually { nu(vm).items[0].read.not() }
        assertEquals(2, nu(vm).unread)
    }

    @Test fun markAllReadClearsTheBadgeAndRestoresOnFailure() = runTest {
        setup()
        answers["GET /api/v1/notifications?page=0"] = ok(notesJson)
        answers["PUT /api/v1/notifications/read-all"] = { MockResponse().setResponseCode(204) }
        val vm = notes()
        eventually { vm.state.value is UiState.Content }
        vm.markAllRead()
        eventually { nu(vm).unread == 0 }

        val failing = notes()
        eventually { failing.state.value is UiState.Content }
        answers["PUT /api/v1/notifications/read-all"] = { json("""{"code":"BOOM"}""", 500) }
        failing.markAllRead()
        eventually { nu(failing).unread == 2 && nu(failing).problem != null }
    }

    private fun profile(firebase: FakeFirebaseAuth = FakeFirebaseAuth()) = ProfileViewModel(AccountRepository(api()), SessionRepository(api(), tokens, firebase, FakePendingEmail(), settings), settings)
    private fun content(vm: ProfileViewModel) = (vm.state.value as UiState.Content).data

    @Test fun theProfileShowsTheEmailOfAnEmailAccountAndOffersAPasswordReset() = runTest {
        setup()
        answers["GET /api/v1/auth/me"] = ok("""{"id":7,"username":"Minh","email":"minh@example.com","roles":["USER"]}""")
        val firebase = FakeFirebaseAuth()
        val vm = profile(firebase)
        eventually { vm.state.value is UiState.Content }
        assertEquals("minh@example.com", content(vm).profile.email)
        assertNull(content(vm).profile.phone)
        assertTrue(content(vm).canResetPassword)
        vm.sendPasswordReset()
        eventually { content(vm).message == ProfileMessage.ResetSent }
        assertEquals(listOf("reset:minh@example.com"), firebase.calls)
    }

    @Test fun aPhoneAccountShowsItsNumberNeverTheInternalAddressAndHasNoPassword() = runTest {
        setup()
        answers["GET /api/v1/auth/me"] = ok("""{"id":8,"username":"Lan","email":"p84912345678@phone.ceta.invalid","roles":["USER"]}""")
        val vm = profile()
        eventually { vm.state.value is UiState.Content }
        assertEquals("+84912345678", content(vm).profile.phone)
        assertNull(content(vm).profile.email)
        assertFalse(content(vm).canResetPassword)
    }

    @Test fun renamingSavesOnTheServerAndUpdatesTheNameShownAroundTheApp() = runTest {
        setup()
        answers["GET /api/v1/auth/me"] = ok("""{"id":7,"username":"Minh","email":"minh@example.com"}""")
        answers["PATCH /api/v1/auth/me"] = ok("""{"id":7,"username":"Minh Nguyen","email":"minh@example.com"}""")
        val vm = profile()
        eventually { vm.state.value is UiState.Content }
        vm.saveName("  Minh Nguyen ")
        eventually { content(vm).message == ProfileMessage.Saved }
        assertEquals("Minh Nguyen", content(vm).profile.username)
        assertEquals("Minh Nguyen", kotlinx.coroutines.runBlocking { settings.settings.first().displayName })
    }

    @Test fun aTooShortNameIsRefusedWithoutAskingTheServer() = runTest {
        setup()
        answers["GET /api/v1/auth/me"] = ok("""{"id":7,"username":"Minh","email":"minh@example.com"}""")
        val vm = profile()
        eventually { vm.state.value is UiState.Content }
        vm.saveName("Mi")
        assertEquals(ProfileMessage.NameInvalid, content(vm).message)
        assertTrue(requests.none { it.startsWith("PATCH") })
    }

    @Test fun removingAVoucherTakesItOutAtOnceAndAnEmptyWalletIsAnEmptyState() = runTest {
        setup()
        answers["GET /api/v1/vouchers"] = ok("""[{"promotion":{"id":1,"code":"CETA20","title":"New","kind":"PERCENT_OFF"}}]""")
        answers["DELETE /api/v1/vouchers/1"] = { MockResponse().setResponseCode(204) }
        val vm = VouchersViewModel(AccountRepository(api()))
        eventually { vm.state.value is UiState.Content }
        vm.remove(1)
        assertTrue(vm.state.value is UiState.Empty)
        eventually { requests.any { it.startsWith("DELETE /api/v1/vouchers/1") } }
    }

    @Test fun theReminderListShowsMoviesAndDropsOneOnRemove() = runTest {
        setup()
        answers["GET /api/v1/reminders"] = ok("""[{"id":"m1","title":"Dune 3","posterUrl":null},{"id":"m2","title":"Zorro"}]""")
        answers["DELETE /api/v1/movies/m1/reminder"] = { MockResponse().setResponseCode(204) }
        val vm = RemindersViewModel(AccountRepository(api()))
        eventually { vm.state.value is UiState.Content }
        assertEquals(listOf("Dune 3", "Zorro"), (vm.state.value as UiState.Content).data.map { it.title })
        vm.remove("m1")
        assertEquals(listOf("m2"), (vm.state.value as UiState.Content).data.map { it.id })
    }

    @Test fun initialsAreTheFirstAndLastWord() {
        assertEquals("HT", initialsOf("Huynh Quco Trung"))
        assertEquals("M", initialsOf("Minh"))
        assertEquals("C", initialsOf(null))
        assertEquals("C", initialsOf("   "))
    }
}
