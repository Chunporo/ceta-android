package com.ceta.cinema.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Emits when the refresh token no longer works; the nav host sends the user back to Login. */
@Singleton
class SessionEvents @Inject constructor() {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val expired: SharedFlow<Unit> = _expired

    fun notifyExpired() {
        _expired.tryEmit(Unit)
    }
}
