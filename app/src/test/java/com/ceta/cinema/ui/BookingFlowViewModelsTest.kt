package com.ceta.cinema.ui

import androidx.lifecycle.SavedStateHandle
import com.ceta.cinema.core.OfflineFlag
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.*
import com.ceta.cinema.data.repository.AccountRepository
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.data.repository.TicketRepository
import com.ceta.cinema.domain.usecase.PaymentOutcome
import com.ceta.cinema.ui.screen.booking.*
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
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalCoroutinesApi::class)
class BookingFlowViewModelsTest {
    private lateinit var server: MockWebServer
    private val offline = OfflineFlag()
    private val tokens = FakeTokenStore("access", "refresh")
    private val settings = FakeSettingsStore()
    private val clock = TestClock(Instant.parse("2026-11-18T10:00:00Z"))
    private val requests = CopyOnWriteArrayList<String>()
    private val bodies = CopyOnWriteArrayList<String>()
    private val answers = ConcurrentHashMap<String, () -> MockResponse>()

    @Before fun start() {
        server = MockWebServer().also { s ->
            s.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    requests += "${request.method} ${request.path}"
                    bodies += request.body.readUtf8()
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

    private fun booking(
        status: String = "PENDING", secs: Long = 540, combos: String = "[]", comboAmount: Long = 0, discount: Long = 0, voucher: String? = null,
        points: Long = 0, pointsUsed: Int = 0, refund: Boolean = false,
    ): String {
        val total = 170_000 + comboAmount - discount - points
        return """{"id":42,"status":"$status","holdExpiresAt":"${clock.now.plusSeconds(secs)}","secondsRemaining":$secs,"totalAmount":$total,
            "seats":[{"seatId":1,"label":"F7","price":85000},{"seatId":2,"label":"F8","price":85000}],"tickets":[],"refundRequired":$refund,
            "comboAmount":$comboAmount,"combos":$combos,"discountAmount":$discount,"voucherCode":${voucher?.let { "\"$it\"" } ?: "null"},"pointsAmount":$points,"pointsUsed":$pointsUsed}"""
    }
    private val menu = """[{"id":2,"name":"Combo Đôi","price":119000,"active":true},{"id":5,"name":"Nước ngọt (L)","price":35000,"active":true}]"""
    private fun loyalty(balance: Long = 2450) = """{"balance":$balance,"lifetimeEarned":$balance,"tier":"MEMBER","nextTier":"VIP","pointsToNextTier":2550,"redeemBlockPoints":2000,"redeemBlockVnd":20000,"stats":{}}"""
    private val extrasOk = """{"bookingId":42,"totalAmount":170000,"comboAmount":0,"discountAmount":0,"combos":[],"pointsAmount":0,"pointsUsed":0}"""

    // ---- snacks step

    private fun combo() = ComboViewModel(SavedStateHandle(mapOf("bookingId" to 42L)), BookingRepository(api(), settings), CatalogRepository(api(), offline), clock)
    private fun ui(vm: ComboViewModel) = (vm.state.value as UiState.Content).data

    @Test fun theMenuLoadsWithEarlierChoicesAndAPreviewTotal() = runTest {
        setup()
        answers["GET /api/v1/bookings/42"] = ok(booking(combos = """[{"comboId":2,"name":"Combo Đôi","quantity":1,"unitPrice":119000}]""", comboAmount = 119_000))
        answers["GET /api/v1/combos"] = ok(menu)
        val vm = combo()
        eventually { vm.state.value is UiState.Content }
        assertEquals(mapOf(2L to 1), ui(vm).quantities)
        assertEquals(2, ui(vm).combos.size)
        assertEquals(119_000L, ui(vm).snacksTotal)
        assertEquals(170_000L + 119_000L, ui(vm).total)
        assertEquals(540, ui(vm).secondsLeft)
    }

    @Test fun quantitiesStayBetweenZeroAndTen() = runTest {
        setup()
        answers["GET /api/v1/bookings/42"] = ok(booking())
        answers["GET /api/v1/combos"] = ok(menu)
        val vm = combo()
        eventually { vm.state.value is UiState.Content }
        vm.change(5, -1)
        assertEquals(0, ui(vm).quantities[5] ?: 0)
        repeat(12) { vm.change(5, +1) }
        assertEquals(10, ui(vm).quantities[5])
        assertEquals(10 * 35_000L, ui(vm).snacksTotal)
    }

    @Test fun continuingSavesTheChosenSnacksKeepingVoucherAndPointsThenMovesOn() = runTest {
        setup()
        answers["GET /api/v1/bookings/42"] = ok(booking(voucher = "CETA20", discount = 17_000, points = 20_000, pointsUsed = 2000))
        answers["GET /api/v1/combos"] = ok(menu)
        answers["PUT /api/v1/bookings/42/extras"] = ok(extrasOk)
        val vm = combo()
        eventually { vm.state.value is UiState.Content }
        vm.change(2, +2)

        vm.proceed(skip = false)

        vm.next.first()
        val body = bodies.last { it.contains("usePoints") }
        assertTrue(body, body.contains("\"comboId\":2") && body.contains("\"quantity\":2"))
        assertTrue(body, body.contains("\"voucherCode\":\"CETA20\"") && body.contains("\"usePoints\":true"))
    }

    @Test fun skippingSavesNoSnacks() = runTest {
        setup()
        answers["GET /api/v1/bookings/42"] = ok(booking())
        answers["GET /api/v1/combos"] = ok(menu)
        answers["PUT /api/v1/bookings/42/extras"] = ok(extrasOk)
        val vm = combo()
        eventually { vm.state.value is UiState.Content }
        vm.change(2, +3)
        vm.proceed(skip = true)
        vm.next.first()
        assertFalse(bodies.last { it.contains("usePoints") }.contains("\"comboId\""))
    }

    @Test fun aMenuThatCannotLoadDoesNotBlockCheckout() = runTest {
        setup()
        answers["GET /api/v1/bookings/42"] = ok(booking())
        answers["PUT /api/v1/bookings/42/extras"] = ok(extrasOk)
        val vm = combo()
        eventually { vm.state.value is UiState.Content }
        assertTrue(ui(vm).combos.isEmpty())
        vm.proceed(skip = true)
        vm.next.first()
    }

    @Test fun anOrderWhosePaymentAlreadyStartedJustGoesOnToPay() = runTest {
        setup()
        answers["GET /api/v1/bookings/42"] = ok(booking())
        answers["GET /api/v1/combos"] = ok(menu)
        answers["PUT /api/v1/bookings/42/extras"] = { json("""{"code":"PAYMENT_ALREADY_STARTED","message":"x","details":[]}""", 409) }
        val vm = combo()
        eventually { vm.state.value is UiState.Content }
        vm.proceed(skip = true)
        vm.next.first()
    }

    @Test fun anExpiredHoldIsReportedAndNothingIsSent() = runTest {
        setup()
        answers["GET /api/v1/bookings/42"] = ok(booking(secs = 540))
        answers["GET /api/v1/combos"] = ok(menu)
        answers["PUT /api/v1/bookings/42/extras"] = { json("""{"code":"HOLD_EXPIRED","message":"x","details":[]}""", 409) }
        val vm = combo()
        eventually { vm.state.value is UiState.Content }
        vm.proceed(skip = false)
        eventually { ui(vm).expired }
        assertEquals("HOLD_EXPIRED", ui(vm).error?.code)
        requests.clear()
        vm.proceed(skip = false)
        assertTrue("an expired order sends nothing more", requests.isEmpty())
    }

    @Test fun theCountdownFollowsTheServerAndHitsZeroWhenTheHoldEnds() = runTest {
        setup()
        answers["GET /api/v1/bookings/42"] = ok(booking(secs = 130))
        answers["GET /api/v1/combos"] = ok(menu)
        val vm = combo()
        eventually { vm.state.value is UiState.Content }
        assertFalse(ui(vm).warning)
        clock.advance(20); vm.tick()
        assertEquals(110, ui(vm).secondsLeft)
        assertTrue(ui(vm).warning)
        clock.advance(200); vm.tick()
        assertTrue(ui(vm).expired)
        assertEquals(0, ui(vm).secondsLeft)
    }

    // ---- checkout

    private fun checkout() = CheckoutViewModel(SavedStateHandle(mapOf("bookingId" to 42L)), BookingRepository(api(), settings), AccountRepository(api()), clock)
    private fun co(vm: CheckoutViewModel) = (vm.state.value as UiState.Content).data

    private fun checkoutApis(booking: String = booking(), balance: Long = 2450) {
        answers["GET /api/v1/bookings/42"] = ok(booking)
        answers["GET /api/v1/me/loyalty"] = ok(loyalty(balance))
    }

    @Test fun checkoutShowsTheOrderPointsAndWhetherPointsCanBeUsed() = runTest {
        setup()
        checkoutApis()
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }
        assertEquals(170_000L, co(vm).booking.totalAmount)
        assertTrue(co(vm).canUsePoints)
        assertFalse(co(vm).usingPoints)
        assertTrue(co(vm).canPay)
    }

    @Test fun pointsCannotBeUsedWithoutEnoughOfThem() = runTest {
        setup()
        checkoutApis(balance = 1999)
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }
        assertFalse(co(vm).canUsePoints)
    }

    @Test fun aValidVoucherLowersTheTotalAndAnInvalidOneIsExplainedWithoutLosingTheOldOne() = runTest {
        setup()
        checkoutApis()
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }

        answers["PUT /api/v1/bookings/42/extras"] = ok(extrasOk)
        answers["GET /api/v1/bookings/42"] = ok(booking(voucher = "CETA20", discount = 17_000))
        vm.onVoucher("ceta20")
        assertEquals("CETA20", co(vm).voucherInput)
        vm.applyVoucher()
        eventually { co(vm).booking.voucherCode == "CETA20" }
        assertEquals(153_000L, co(vm).booking.totalAmount)
        assertFalse(co(vm).working)

        answers["PUT /api/v1/bookings/42/extras"] = { json("""{"code":"VOUCHER_INVALID","message":"x","details":[]}""", 400) }
        vm.onVoucher("NOPE"); vm.applyVoucher()
        eventually { co(vm).voucherError?.code == "VOUCHER_INVALID" }
        assertEquals("the earlier voucher stays", "CETA20", co(vm).booking.voucherCode)
        assertTrue(bodies.any { it.contains("\"voucherCode\":\"CETA20\"") })
    }

    @Test fun removingTheVoucherSendsNone() = runTest {
        setup()
        checkoutApis(booking(voucher = "CETA20", discount = 17_000))
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }
        answers["PUT /api/v1/bookings/42/extras"] = ok(extrasOk)
        answers["GET /api/v1/bookings/42"] = ok(booking())
        vm.removeVoucher()
        eventually { co(vm).booking.voucherCode == null && !co(vm).working }
        assertTrue(bodies.last { it.contains("usePoints") }.contains("\"voucherCode\":null") || !bodies.last { it.contains("usePoints") }.contains("voucherCode\":\""))
    }

    @Test fun usingPointsTakesOffTheirValueAndTurningThemOffGivesItBack() = runTest {
        setup()
        checkoutApis()
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }
        answers["PUT /api/v1/bookings/42/extras"] = ok(extrasOk)
        answers["GET /api/v1/bookings/42"] = ok(booking(points = 20_000, pointsUsed = 2000))
        vm.togglePoints()
        eventually { co(vm).usingPoints && !co(vm).working }
        assertEquals(150_000L, co(vm).booking.totalAmount)
        assertTrue(bodies.last { it.contains("usePoints") }.contains("\"usePoints\":true"))

        answers["GET /api/v1/bookings/42"] = ok(booking())
        vm.togglePoints()
        eventually { !co(vm).usingPoints && !co(vm).working }
        assertEquals(170_000L, co(vm).booking.totalAmount)
        assertTrue(bodies.last { it.contains("usePoints") }.contains("\"usePoints\":false"))
    }

    @Test fun notEnoughPointsIsExplainedAndNothingChanges() = runTest {
        setup()
        checkoutApis()
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }
        answers["PUT /api/v1/bookings/42/extras"] = { json("""{"code":"INSUFFICIENT_POINTS","message":"x","details":[]}""", 409) }
        vm.togglePoints()
        eventually { co(vm).error?.code == "INSUFFICIENT_POINTS" }
        assertFalse(co(vm).usingPoints)
    }

    @Test fun payingOpensTheVnpayPageOnceEvenOnADoubleTap() = runTest {
        setup()
        checkoutApis()
        answers["POST /api/v1/payments/vnpay/create-url"] = { json("""{"payUrl":"https://sandbox.vnpayment.vn/pay?x=1","txnRef":"42_1"}""").setBodyDelay(200, java.util.concurrent.TimeUnit.MILLISECONDS) }
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }

        vm.pay(); vm.pay()

        val event = vm.events.first() as CheckoutEvent.OpenPayment
        assertEquals("https://sandbox.vnpayment.vn/pay?x=1", event.url)
        assertEquals(42L, event.bookingId)
        assertEquals(1, requests.count { it.contains("create-url") })
    }

    @Test fun checkoutOffersOnlyVnpayWhenTheServerCannotBeAskedOrMomoIsOff() = runTest {
        setup()
        checkoutApis()
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }
        assertEquals(listOf(com.ceta.cinema.domain.model.PaymentProvider.VNPAY), co(vm).methods)

        answers["GET /api/v1/payments/methods"] = ok("""[{"code":"VNPAY","enabled":true},{"code":"MOMO","enabled":false}]""")
        val off = checkout()
        eventually { off.state.value is UiState.Content }
        assertEquals(listOf(com.ceta.cinema.domain.model.PaymentProvider.VNPAY), co(off).methods)
    }

    @Test fun momoAppearsWhenOnAndPayingWithItAsksTheServerForAMomoPayment() = runTest {
        setup()
        checkoutApis()
        answers["GET /api/v1/payments/methods"] = ok("""[{"code":"VNPAY","enabled":true},{"code":"MOMO","enabled":true},{"code":"ZALOPAY","enabled":true}]""")
        answers["POST /api/v1/payments/vnpay/create-url"] = ok("""{"payUrl":"http://momo.test/pay?orderId=42_1","txnRef":"42_1"}""")
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }
        assertEquals(listOf(com.ceta.cinema.domain.model.PaymentProvider.VNPAY, com.ceta.cinema.domain.model.PaymentProvider.MOMO), co(vm).methods)
        assertEquals(com.ceta.cinema.domain.model.PaymentProvider.VNPAY, co(vm).method)

        vm.selectMethod(com.ceta.cinema.domain.model.PaymentProvider.MOMO)
        assertEquals(com.ceta.cinema.domain.model.PaymentProvider.MOMO, co(vm).method)
        vm.pay()

        val event = vm.events.first() as CheckoutEvent.OpenPayment
        assertEquals("http://momo.test/pay?orderId=42_1", event.url)
        assertTrue("the request names MoMo", bodies.any { it.contains("\"bookingId\":42") && it.contains("\"provider\":\"MOMO\"") })
    }

    @Test fun vnpayStaysTheDefaultAndSendsNoProvider() = runTest {
        setup()
        checkoutApis()
        answers["POST /api/v1/payments/vnpay/create-url"] = ok("""{"payUrl":"https://sandbox.vnpayment.vn/pay","txnRef":"42_1"}""")
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }
        vm.pay()
        vm.events.first()
        val sent = bodies.firstOrNull { it.contains("bookingId") }
        assertNotNull(sent)
        assertFalse("the default needs no provider field: $sent", sent!!.contains("provider"))
    }

    @Test fun aMethodThatIsNotOfferedCannotBeSelected() = runTest {
        setup()
        checkoutApis()
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }
        vm.selectMethod(com.ceta.cinema.domain.model.PaymentProvider.MOMO)
        assertEquals(com.ceta.cinema.domain.model.PaymentProvider.VNPAY, co(vm).method)
    }

    @Test fun anExpiredHoldCannotBePaid() = runTest {
        setup()
        checkoutApis(booking(secs = 30))
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }
        clock.advance(60); vm.tick()
        assertTrue(co(vm).expired)
        assertFalse(co(vm).canPay)
        vm.pay()
        assertTrue(requests.none { it.contains("create-url") })
        vm.applyVoucher(); vm.togglePoints()
        assertTrue(requests.none { it.startsWith("PUT") })
    }

    @Test fun aFailureToGetThePaymentLinkIsShownAndPayingCanBeTriedAgain() = runTest {
        setup()
        checkoutApis()
        answers["POST /api/v1/payments/vnpay/create-url"] = { json("""{"code":"PAYMENT_PROVIDER_ERROR","message":"x","details":[]}""", 502) }
        val vm = checkout()
        eventually { vm.state.value is UiState.Content }
        vm.pay()
        eventually { co(vm).error?.code == "PAYMENT_PROVIDER_ERROR" }
        assertTrue(co(vm).canPay)
    }

    @Test fun aBookingThatCannotBeLoadedIsAnError() = runTest {
        setup()
        val vm = checkout()
        eventually { vm.state.value is UiState.Error }
    }

    // ---- payment result

    private fun result() = PaymentResultViewModel(SavedStateHandle(mapOf("bookingId" to 42L)), BookingRepository(api(), settings), TicketRepository(api(), offline))

    private fun sequence(vararg bodies: String): () -> MockResponse {
        var i = 0
        return { json(bodies[minOf(i++, bodies.size - 1)]) }
    }

    /** Moves virtual time forward in small steps until [cond] holds: each real HTTP answer must land before the next delay can fire. */
    private fun TestScope.pump(timeoutMs: Long = 8_000, cond: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!cond() && System.currentTimeMillis() < end) { advanceTimeBy(1_000); Thread.sleep(15) }
        assertTrue("condition not met within ${timeoutMs}ms", cond())
    }

    @Test fun thePaymentIsShownAsSuccessOnlyOnceTheServerSaysPaid() = runTest {
        setup()
        settings.setPendingBooking(42, 1)
        answers["GET /api/v1/bookings/42"] = sequence(booking("PENDING"), booking("PENDING"), booking("PAID"))
        val vm = result()
        eventually { requests.size >= 1 }
        assertEquals(PaymentOutcome.PROCESSING, vm.state.value.outcome)
        pump { requests.size >= 2 }
        assertNotEquals("still pending is never success", PaymentOutcome.PAID, vm.state.value.outcome)
        pump { vm.state.value.outcome == PaymentOutcome.PAID }
        assertNull("the pending order is forgotten once it ended", settings.current.pendingBookingId)
        val calls = requests.size
        advanceTimeBy(60_000)
        Thread.sleep(100)
        assertEquals("polling stops at the end state", calls, requests.size)
    }

    @Test fun aLatePaymentOnAnExpiredHoldIsAnnouncedAsARefund() = runTest {
        setup()
        answers["GET /api/v1/bookings/42"] = ok(booking("EXPIRED", refund = true))
        val vm = result()
        eventually { vm.state.value.outcome == PaymentOutcome.REFUND }
    }

    @Test fun expiredAndCancelledOrdersEndWithTheirOwnMessage() = runTest {
        setup()
        answers["GET /api/v1/bookings/42"] = ok(booking("EXPIRED"))
        assertEquals(PaymentOutcome.EXPIRED, result().let { vm -> eventually { vm.state.value.outcome == PaymentOutcome.EXPIRED }; vm.state.value.outcome })
        answers["GET /api/v1/bookings/42"] = ok(booking("CANCELLED"))
        assertEquals(PaymentOutcome.CANCELLED, result().let { vm -> eventually { vm.state.value.outcome == PaymentOutcome.CANCELLED }; vm.state.value.outcome })
    }

    @Test fun afterAboutAMinuteWithoutAnAnswerItStopsSpinningAndOffersACheck() = runTest {
        setup()
        answers["GET /api/v1/bookings/42"] = ok(booking("PENDING"))
        val vm = result()
        pump(timeoutMs = 15_000) { vm.state.value.outcome == PaymentOutcome.STILL_PENDING }

        answers["GET /api/v1/bookings/42"] = ok(booking("PAID"))
        vm.checkAgain()
        pump { vm.state.value.outcome == PaymentOutcome.PAID }
    }

    @Test fun aNetworkHiccupDoesNotEndThePolling() = runTest {
        setup()
        var calls = 0
        answers["GET /api/v1/bookings/42"] = { if (calls++ == 0) MockResponse().setResponseCode(500).setBody("""{"code":"BOOM"}""") else json(booking("PAID")) }
        val vm = result()
        eventually { calls >= 1 }
        pump { vm.state.value.outcome == PaymentOutcome.PAID }
    }
}
