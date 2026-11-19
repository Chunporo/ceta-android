package com.ceta.cinema.domain.usecase

import java.time.Duration
import java.time.Instant

/**
 * The countdown of a seat hold. It is driven by the server's deadline and re-aimed by the server's own "seconds remaining"
 * on every poll, never by trusting the phone's clock alone (a customer who changes the time must not get more minutes).
 */
class HoldClock(private val now: () -> Instant = Instant::now) {
    private var deadline: Instant? = null

    fun start(expiresAt: Instant) { deadline = expiresAt }

    /** Starts a hold of [ttl] from now (the seat screen's own countdown, which begins at the first chosen seat). */
    fun startFor(ttl: Duration) { deadline = now().plus(ttl) }

    /** Ends the countdown (every seat was un-chosen). */
    fun stop() { deadline = null }

    fun isRunning(): Boolean = deadline != null

    /** Re-aim the deadline from the server's seconds-remaining, measured from this moment. */
    fun sync(serverSecondsRemaining: Long) { deadline = now().plusSeconds(serverSecondsRemaining.coerceAtLeast(0)) }

    fun remaining(): Duration = deadline?.let { Duration.between(now(), it).coerceAtLeast(Duration.ZERO) } ?: Duration.ZERO

    fun remainingSeconds(): Long = remaining().seconds

    fun isExpired(): Boolean = deadline != null && remaining().isZero

    /** The last two minutes turn the chip yellow and offer "pay now". */
    fun isWarning(): Boolean = deadline != null && !isExpired() && remaining() <= WARNING

    companion object {
        val WARNING: Duration = Duration.ofSeconds(120)
        val HOLD: Duration = Duration.ofMinutes(10)
    }
}

private fun Duration.coerceAtLeast(min: Duration): Duration = if (this < min) min else this
