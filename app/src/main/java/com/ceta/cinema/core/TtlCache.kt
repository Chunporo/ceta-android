package com.ceta.cinema.core

import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * A small in-memory cache: catalog data for 6 hours, tickets for 5 minutes, and never the seat map (the seat map must
 * always come from the network). Entries are replaced only by a successful fetch, so a failed refresh still has the old one.
 */
class TtlCache<K : Any, V : Any>(private val ttl: Duration, private val clock: Clock = Clock.systemUTC()) {
    private class Entry<V>(val value: V, val storedAt: Long)

    private val entries = ConcurrentHashMap<K, Entry<V>>()

    /** The value if it is younger than the TTL. */
    fun fresh(key: K): V? = entries[key]?.takeIf { clock.millis() - it.storedAt < ttl.toMillis() }?.value

    /** The value however old it is: what to show when offline. */
    fun stale(key: K): V? = entries[key]?.value

    fun put(key: K, value: V) { entries[key] = Entry(value, clock.millis()) }

    fun remove(key: K) { entries.remove(key) }

    fun clear() { entries.clear() }
}
