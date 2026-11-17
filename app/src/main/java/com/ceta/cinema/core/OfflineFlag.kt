package com.ceta.cinema.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** True while the screen is showing saved catalog data because the network failed: the design's "viewing saved data" banner. */
@Singleton
class OfflineFlag @Inject constructor() {
    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline
    fun set(value: Boolean) { _offline.value = value }
}
