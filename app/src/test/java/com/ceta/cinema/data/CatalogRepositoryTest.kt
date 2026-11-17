package com.ceta.cinema.data

import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.OfflineFlag
import com.ceta.cinema.data.repository.AccountRepository
import com.ceta.cinema.data.repository.BookingRepository
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.data.repository.TicketRepository
import com.ceta.cinema.domain.model.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CatalogRepositoryTest {
    private lateinit var server: MockWebServer
    private val offline = OfflineFlag()

    @Before fun start() { server = MockWebServer().also { it.start() } }
    @After fun stop() { server.shutdown() }

    private fun api() = apiFor(server, FakeTokenStore("access", "refresh"))
    private fun catalog() = CatalogRepository(api(), offline)
    private fun account() = AccountRepository(api())

    @Test fun moviesMapEveryDesignField() = runTest {
        server.enqueue(json(fixture("movies.json")))

        val movies = (catalog().movies(status = MovieStatus.NOW_SHOWING) as ApiResult.Ok).data

        val m = movies[0]
        assertEquals("11111111-1111-1111-1111-111111111111", m.id)
        assertEquals("T16", m.ageRating)
        assertEquals(MovieStatus.NOW_SHOWING, m.status)
        assertEquals(128, m.durationMin)
        assertEquals(LocalDate.of(2026, 11, 7), m.releaseDate)
        assertEquals("Phạm Long", m.director)
        assertEquals(8.6, m.ratingAverage!!, 0.0001)
        assertEquals(12_400L, m.ratingCount)
        assertEquals(listOf("Hành động", "Phiêu lưu"), m.genres)
        assertEquals(setOf(ShowFormat.TWO_D, ShowFormat.IMAX), m.formats)
        assertTrue(m.isNowShowing)
        val soon = movies[1]
        assertNull(soon.ratingAverage)
        assertEquals(MovieStatus.COMING_SOON, soon.status)
        assertFalse(soon.isNowShowing)
        assertEquals("/api/v1/movies?status=NOW_SHOWING&size=20", server.takeRequest().path)
    }

    @Test fun aSearchSendsItsQueryAndGenre() = runTest {
        server.enqueue(json("[]"))
        catalog().movies(q = " dune ", genre = "hanh-dong")
        val path = server.takeRequest().path!!
        assertTrue(path, path.contains("q=%20dune%20") || path.contains("q=+dune+") || path.contains("q=dune"))
        assertTrue(path, path.contains("genre=hanh-dong"))
    }

    @Test fun theSecondAskIsServedFromTheCacheWithoutANetworkCall() = runTest {
        server.enqueue(json(fixture("movies.json")))
        val repo = catalog()
        repo.movies()
        repo.movies()
        assertEquals(1, server.requestCount)
    }

    @Test fun whenTheNetworkFailsOldDataIsShownAndTheOfflineFlagIsRaised() = runTest {
        server.enqueue(json(fixture("movies.json")))
        val repo = catalog()
        repo.movies(q = "a")
        repo.forgetMovie("none")           // clears the list cache so the next call must fetch
        server.shutdown()
        // the cache was cleared by forgetMovie, so nothing old exists: a real error, not stale data
        val result = repo.movies(q = "a")
        assertEquals("NETWORK", (result as ApiResult.Err).error.code)
        assertFalse(offline.offline.value)
    }

    @Test fun showtimesCarryCinemaFormatAndLanguage() = runTest {
        server.enqueue(json(fixture("showtimes.json")))

        val list = (catalog().showtimesForMovie("11111111-1111-1111-1111-111111111111", LocalDate.of(2026, 11, 18)) as ApiResult.Ok).data

        val a = list[0]
        assertEquals(501, a.id)
        assertEquals("CETA Landmark", a.cinemaName)
        assertEquals(3L, a.cinemaId)
        assertEquals(ShowFormat.IMAX, a.format)
        assertEquals(LanguageMode.DUB, a.languageMode)
        assertEquals(Instant.parse("2026-11-18T12:30:00Z"), a.start)
        assertEquals(85_000L, a.basePrice)
        assertFalse(a.cancelled)
        val b = list[1]
        assertTrue(b.cancelled)
        assertEquals(ShowFormat.TWO_D, b.format)
        assertEquals(LanguageMode.SUB, b.languageMode)
        assertNull(b.cinemaName)
        val path = server.takeRequest().path!!
        assertTrue(path, path.contains("movieId=11111111-1111-1111-1111-111111111111") && path.contains("date=2026-11-18"))
    }

    @Test fun cinemaShowtimesAskByCinema() = runTest {
        server.enqueue(json("[]"))
        catalog().showtimesAtCinema(3, LocalDate.of(2026, 11, 18))
        val path = server.takeRequest().path!!
        assertTrue(path, path.contains("cinemaId=3") && path.contains("date=2026-11-18"))
    }

    @Test fun combosAndOffersAreMappedAndLocalised() = runTest {
        server.enqueue(json("""[{"id":2,"code":"COUPLE","name":"Combo Đôi","nameEn":"Couple Combo","description":"d","descriptionEn":"e","price":119000,"active":true}]"""))
        server.enqueue(json("""[{"id":1,"code":"CETA20","title":"Thành viên mới -20%","titleEn":"New members -20%","kind":"PERCENT_OFF","percentOff":20,"maxDiscount":50000,"validUntil":"2027-01-31T16:59:59Z"},
            {"id":4,"code":"HSSV49","title":"Combo sinh viên","kind":"COUNTER_PERK"},{"id":9,"code":"X","title":"x","kind":"SOMETHING_NEW"}]"""))
        val combo = (catalog().combos() as ApiResult.Ok).data[0]
        assertEquals("Couple Combo", combo.title(english = true))
        assertEquals("Combo Đôi", combo.title(english = false))
        assertEquals(119_000L, combo.price)
        val offers = (catalog().offers() as ApiResult.Ok).data
        assertEquals("New members -20%", offers[0].title(true))
        assertEquals("no English copy falls back to the Vietnamese", "Combo sinh viên", offers[1].title(true))
        assertTrue(offers[0].applicableOnline)
        assertFalse(offers[1].applicableOnline)
        assertFalse("an unknown kind is never offered at checkout", offers[2].applicableOnline)
        assertEquals(Instant.parse("2027-01-31T16:59:59Z"), offers[0].validUntil)
    }

    @Test fun aListTheServerLeavesOutBecomesEmptyNotNull() = runTest {
        // no seats, tickets, combos at all: Gson alone would leave them null and the mapper would crash
        server.enqueue(json("""{"id":42,"status":"PENDING","holdExpiresAt":null,"secondsRemaining":0,"totalAmount":1000}"""))
        val booking = (BookingRepository(api()).get(42) as ApiResult.Ok).data
        assertTrue(booking.seats.isEmpty() && booking.tickets.isEmpty() && booking.combos.isEmpty())
        assertEquals(0L, booking.comboAmount)
        assertEquals(0L, booking.pointsAmount)
    }

    @Test fun aReplyOfTheWrongShapeIsAnErrorNotACrash() = runTest {
        server.enqueue(json("{\"unexpected\":\"an object where a list was expected\"}"))
        server.enqueue(json("[1,2,3]"))
        val repo = catalog()
        assertEquals("UNKNOWN", (repo.movies(q = "a") as ApiResult.Err).error.code)
        assertEquals("UNKNOWN", (repo.cinemas() as ApiResult.Err).error.code)
    }

    @Test fun loyaltyMapsPointsTierAndCounters() = runTest {
        server.enqueue(json(fixture("loyalty.json")))
        val l = (account().loyalty() as ApiResult.Ok).data
        assertEquals(2450, l.balance)
        assertEquals("VIP", l.nextTier)
        assertEquals(2550, l.pointsToNextTier)
        assertTrue(l.canRedeem)
        assertEquals(12, l.ticketsWatched); assertEquals(3, l.vouchers); assertEquals(5, l.reviews)
    }

    @Test fun reviewsOnlyForThoseWhoWatchedAreRefusedWithTheirOwnCode() = runTest {
        server.enqueue(json("""{"code":"REVIEW_NOT_ALLOWED","message":"x","details":[]}""", 403))
        val error = (account().saveReview("m1", 8, "Hay") as ApiResult.Err).error
        assertEquals("REVIEW_NOT_ALLOWED", error.code)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/movies/m1/review", request.path)
        assertTrue(request.body.readUtf8().contains("\"rating\":8"))
    }

    @Test fun walletAndRemindersReturnIds() = runTest {
        server.enqueue(json("""[{"promotion":{"id":2,"code":"A","title":"a","kind":"PERCENT_OFF"},"savedAt":"2026-11-01T00:00:00Z"},{"promotion":{"id":5,"code":"B","title":"b","kind":"COUNTER_PERK"}}]"""))
        server.enqueue(json("""[{"id":"m9","title":"Soon"}]"""))
        assertEquals(setOf(2L, 5L), (account().walletPromotionIds() as ApiResult.Ok).data)
        assertEquals(setOf("m9"), (account().reminderMovieIds() as ApiResult.Ok).data)
    }

    @Test fun anAlreadyOnSaleMovieCannotBeRemindedAbout() = runTest {
        server.enqueue(json("""{"code":"ALREADY_ON_SALE","message":"x","details":[]}""", 409))
        assertEquals("ALREADY_ON_SALE", (account().addReminder("m1") as ApiResult.Err).error.code)
    }

    @Test fun extrasSendTheWholeDesiredStateAndMapTheNewTotal() = runTest {
        server.enqueue(json(fixture("extras.json")))

        val extras = (BookingRepository(api()).updateExtras(42, mapOf(2L to 1, 3L to 0), " CETA20 ", usePoints = true) as ApiResult.Ok).data

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/bookings/42/extras", request.path)
        val body = request.body.readUtf8()
        assertTrue(body, body.contains("\"comboId\":2") && body.contains("\"quantity\":1"))
        assertFalse("a zero quantity is not sent", body.contains("\"comboId\":3"))
        assertTrue(body, body.contains("\"voucherCode\":\"CETA20\"") && body.contains("\"usePoints\":true"))
        assertEquals(218_000L, extras.totalAmount)
        assertEquals(17_000L, extras.discountAmount)
        assertEquals(20_000L, extras.pointsAmount)
        assertEquals("CETA20", extras.voucherCode)
        assertEquals(119_000L, extras.combos.sumOf { it.total })
    }

    @Test fun aBlankVoucherIsSentAsNone() = runTest {
        server.enqueue(json(fixture("extras.json")))
        BookingRepository(api()).updateExtras(42, emptyMap(), "   ", usePoints = false)
        assertTrue(server.takeRequest().body.readUtf8().contains("\"usePoints\":false"))
    }

    @Test fun voucherAndPointsRefusalsKeepTheirCodes() = runTest {
        server.enqueue(json("""{"code":"VOUCHER_INVALID","message":"x","details":[]}""", 400))
        server.enqueue(json("""{"code":"INSUFFICIENT_POINTS","message":"x","details":[]}""", 409))
        server.enqueue(json("""{"code":"PAYMENT_ALREADY_STARTED","message":"x","details":[]}""", 409))
        val repo = BookingRepository(api())
        assertEquals("VOUCHER_INVALID", (repo.updateExtras(1, emptyMap(), "NOPE", false) as ApiResult.Err).error.code)
        assertEquals("INSUFFICIENT_POINTS", (repo.updateExtras(1, emptyMap(), null, true) as ApiResult.Err).error.code)
        assertEquals("PAYMENT_ALREADY_STARTED", (repo.updateExtras(1, emptyMap(), null, false) as ApiResult.Err).error.code)
    }

    @Test fun ticketsShowWhatIsPrintedOnThemAndAreKeptForAShortWhile() = runTest {
        server.enqueue(json(fixture("tickets_page.json")))
        val repo = TicketRepository(api(), offline)

        val ticket = (repo.myTickets() as ApiResult.Ok).data[0]
        repo.myTickets()

        assertEquals("F7", ticket.seatLabel)
        assertEquals("CETA Landmark", ticket.cinemaName)
        assertEquals(ShowFormat.IMAX, ticket.format)
        assertTrue(ticket.used)
        assertEquals("CETA1.9.1.sig", ticket.qrPayload)
        assertEquals("the second call is served from the 5 minute cache", 1, server.requestCount)
        server.enqueue(json(fixture("tickets_page.json")))
        repo.myTickets(forceRefresh = true)
        assertEquals(2, server.requestCount)
    }

    @Test fun paymentUrlIsJustTheLink() = runTest {
        server.enqueue(json("""{"payUrl":"https://sandbox.vnpayment.vn/pay?x=1","txnRef":"42_1"}"""))
        assertEquals("https://sandbox.vnpayment.vn/pay?x=1", (BookingRepository(api()).paymentUrl(42) as ApiResult.Ok).data)
    }

    @Test fun notificationsMapMovieAndBookingLinks() = runTest {
        server.enqueue(json("""{"items":[{"id":1,"type":"MOVIE_ON_SALE","title":"Phim đã mở bán vé","body":"b","createdAt":"2026-11-17T01:00:00Z","read":false,"movieId":"m9"},
            {"id":2,"type":"ORDER_PAID","title":"t","body":"b","bookingId":42,"read":true}],"page":0,"size":30,"totalItems":2,"totalPages":1}"""))
        val list = (account().notifications() as ApiResult.Ok).data
        assertEquals("m9", list[0].movieId)
        assertFalse(list[0].read)
        assertEquals(42L, list[1].bookingId)
        assertTrue(list[1].read)
    }
}
