package com.ceta.cinema.data

import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.SessionEvents
import com.ceta.cinema.data.repository.SeatRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class AuthInterceptorTest {
    private lateinit var server: MockWebServer

    @Before fun start() { server = MockWebServer().also { it.start() } }
    @After fun stop() { server.shutdown() }

    private val refreshBody = """{"accessToken":"access-2","refreshToken":"refresh-2","tokenType":"Bearer","expiresInSeconds":900}"""

    @Test fun a_401_refreshes_once_then_the_request_succeeds() = runTest {
        server.enqueue(json("""{"code":"UNAUTHORIZED","message":"expired"}""", 401))
        server.enqueue(json(refreshBody))
        server.enqueue(json(fixture("seats.json")))
        val tokens = FakeTokenStore("access-1", "refresh-1")

        val result = SeatRepository(apiFor(server, tokens)).seats(5)

        assertTrue(result is ApiResult.Ok)
        assertEquals(3, server.requestCount)
        assertEquals("Bearer access-1", server.takeRequest().getHeader("Authorization"))
        val refresh = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", refresh.path)
        assertNull("refresh must not carry the stale bearer", refresh.getHeader("Authorization"))
        assertTrue(refresh.body.readUtf8().contains("refresh-1"))
        assertEquals("Bearer access-2", server.takeRequest().getHeader("Authorization"))
        assertEquals("access-2", tokens.accessToken)
        assertEquals("refresh-2", tokens.refreshToken)
    }

    @Test fun five_concurrent_401s_share_one_refresh() {
        val refreshCalls = AtomicInteger()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path!!.endsWith("/auth/refresh") -> {
                    refreshCalls.incrementAndGet()
                    Thread.sleep(150) // give the other four time to pile up behind the lock
                    json(refreshBody)
                }
                request.getHeader("Authorization") == "Bearer access-2" -> json(fixture("seats.json"))
                else -> json("""{"code":"UNAUTHORIZED"}""", 401)
            }
        }
        val api = apiFor(server, FakeTokenStore("access-1", "refresh-1"))
        val pool = Executors.newFixedThreadPool(5)
        val start = CountDownLatch(1)
        val results = (1..5).map {
            pool.submit<ApiResult<*>> { start.await(); runBlocking { SeatRepository(api).seats(5) } }
        }
        start.countDown()

        assertTrue(results.all { it.get() is ApiResult.Ok })
        assertEquals("exactly one refresh for five requests", 1, refreshCalls.get())
        pool.shutdown()
    }

    @Test fun a_rejected_refresh_clears_tokens_and_ends_the_session() = runTest {
        server.enqueue(json("""{"code":"UNAUTHORIZED"}""", 401))
        server.enqueue(json("""{"code":"UNAUTHORIZED","message":"Refresh token revoked"}""", 401))
        val tokens = FakeTokenStore("access-1", "refresh-1")
        val events = SessionEvents()
        var expiredEvents = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { events.expired.collect { expiredEvents++ } }

        val result = SeatRepository(apiFor(server, tokens, events)).seats(5)

        assertEquals("UNAUTHORIZED", (result as ApiResult.Err).error.code)
        assertNull(tokens.accessToken)
        assertNull(tokens.refreshToken)
        assertEquals("the nav host is told to go back to Login", 1, expiredEvents)
    }

    @Test fun a_network_failure_during_refresh_keeps_the_session() = runTest {
        server.enqueue(json("""{"code":"UNAUTHORIZED"}""", 401))
        server.enqueue(MockResponse().setResponseCode(503))
        val tokens = FakeTokenStore("access-1", "refresh-1")

        val result = SeatRepository(apiFor(server, tokens)).seats(5)

        assertTrue(result is ApiResult.Err)
        assertEquals("refresh token must survive a server hiccup", "refresh-1", tokens.refreshToken)
    }

    @Test fun login_endpoints_never_get_a_bearer() = runTest {
        server.enqueue(json("""{"accessToken":"a","refreshToken":"r"}"""))
        apiFor(server, FakeTokenStore("access-1", "refresh-1"))
            .loginWithFirebase(com.ceta.cinema.data.remote.dto.FirebaseLoginRequest("id-token"))
        assertNull(server.takeRequest().getHeader("Authorization"))
    }
}
