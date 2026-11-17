package com.ceta.cinema.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class TtlCacheTest {
    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant(): Instant = now
    }

    private val start = Instant.parse("2026-11-17T00:00:00Z")

    @Test fun aValueIsFreshUntilTheTtlPasses() {
        val clock = MutableClock(start)
        val cache = TtlCache<String, Int>(Duration.ofMinutes(5), clock)
        cache.put("a", 1)
        assertEquals(1, cache.fresh("a"))
        clock.now = start.plusSeconds(299)
        assertEquals(1, cache.fresh("a"))
        clock.now = start.plusSeconds(300)
        assertNull(cache.fresh("a"))
        assertEquals("an old value is still there for offline use", 1, cache.stale("a"))
    }

    @Test fun removeAndClearForgetValues() {
        val cache = TtlCache<String, Int>(Duration.ofHours(1))
        cache.put("a", 1); cache.put("b", 2)
        cache.remove("a")
        assertNull(cache.stale("a"))
        cache.clear()
        assertNull(cache.stale("b"))
    }

    @Test fun cachedCallFetchesOnceThenServesTheCache() = runTest {
        val cache = TtlCache<String, Int>(Duration.ofHours(1))
        val offline = OfflineFlag()
        var calls = 0
        repeat(3) { cachedCall(cache, "k", offline) { calls++; 7 } }
        assertEquals(1, calls)
    }

    @Test fun aNetworkFailureServesTheOldValueAndRaisesTheOfflineFlag() = runTest {
        val clock = MutableClock(start)
        val cache = TtlCache<String, Int>(Duration.ofHours(6), clock)
        val offline = OfflineFlag()
        cachedCall(cache, "k", offline) { 1 }
        clock.now = start.plus(Duration.ofHours(7))

        val result = cachedCall(cache, "k", offline) { throw java.io.IOException("down") }

        assertEquals(1, (result as ApiResult.Ok).data)
        assertTrue(offline.offline.value)

        val back = cachedCall(cache, "k", offline) { 2 }
        assertEquals(2, (back as ApiResult.Ok).data)
        assertFalse("a good answer clears the banner", offline.offline.value)
    }

    @Test fun aRealServerErrorIsNotHiddenByOldData() = runTest {
        val clock = MutableClock(start)
        val cache = TtlCache<String, Int>(Duration.ofHours(1), clock)
        val offline = OfflineFlag()
        cachedCall(cache, "k", offline) { 1 }
        clock.now = start.plus(Duration.ofHours(2))

        val result = cachedCall(cache, "k", offline) { throw ApiException(ApiError("BOOM", 500)) }

        assertEquals("BOOM", (result as ApiResult.Err).error.code)
        assertFalse(offline.offline.value)
    }

    @Test fun withNothingCachedANetworkFailureIsAnError() = runTest {
        val result = cachedCall(TtlCache<String, Int>(Duration.ofHours(1)), "k", OfflineFlag()) { throw java.io.IOException("down") }
        assertEquals("NETWORK", (result as ApiResult.Err).error.code)
    }
}
