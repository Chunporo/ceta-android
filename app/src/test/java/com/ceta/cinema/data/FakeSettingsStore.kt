package com.ceta.cinema.data

import com.ceta.cinema.data.local.prefs.AppSettings
import com.ceta.cinema.data.local.prefs.SettingsStore
import com.ceta.cinema.data.local.prefs.withRecent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsStore(initial: AppSettings = AppSettings()) : SettingsStore {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = state
    val current: AppSettings get() = state.value
    override suspend fun setDark(dark: Boolean) { state.value = state.value.copy(dark = dark) }
    override suspend fun setLanguage(language: String) { state.value = state.value.copy(language = language) }
    override suspend fun setOnboardingSeen() { state.value = state.value.copy(onboardingSeen = true) }
    override suspend fun setGuestMode(guest: Boolean) { state.value = state.value.copy(guestMode = guest) }
    override suspend fun setDisplayName(name: String?) { state.value = state.value.copy(displayName = name) }
    override suspend fun setPendingBooking(bookingId: Long, holdUntilMillis: Long) { state.value = state.value.copy(pendingBookingId = bookingId, pendingHoldUntilMillis = holdUntilMillis) }
    override suspend fun clearPendingBooking() { state.value = state.value.copy(pendingBookingId = null, pendingHoldUntilMillis = 0) }
    override suspend fun addRecentSearch(query: String) { state.value = state.value.copy(recentSearches = state.value.recentSearches.withRecent(query)) }
    override suspend fun setTicketRating(code: String, stars: Int) { state.value = state.value.copy(ticketRatings = state.value.ticketRatings + (code to stars.coerceIn(1, 5))) }
    override suspend fun clearRecentSearches() { state.value = state.value.copy(recentSearches = emptyList()) }
}
