package com.ceta.cinema.data.local.prefs

import kotlinx.coroutines.flow.Flow

/** Non-sensitive app settings. Dark is the design's default look; Vietnamese its default language. */
data class AppSettings(
    val dark: Boolean = true,
    val language: String = "vi",          // "vi" | "en"
    val onboardingSeen: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    /** The signed-in customer's display name, for the greeting. Cleared on sign-out. */
    val displayName: String? = null,
    /** A booking created but not yet paid, so a killed app can offer to continue (rule 9). */
    val pendingBookingId: Long? = null,
    val pendingHoldUntilMillis: Long = 0,
    /** The customer chose to browse without an account; remembered so a restart does not ask again. */
    val guestMode: Boolean = false,
)

interface SettingsStore {
    val settings: Flow<AppSettings>
    suspend fun setDark(dark: Boolean)
    suspend fun setLanguage(language: String)
    suspend fun setOnboardingSeen()
    suspend fun setGuestMode(guest: Boolean)
    suspend fun setDisplayName(name: String?)
    suspend fun setPendingBooking(bookingId: Long, holdUntilMillis: Long)
    suspend fun clearPendingBooking()
    /** Most recent first, no duplicates, at most [MAX_RECENT]. */
    suspend fun addRecentSearch(query: String)
    suspend fun clearRecentSearches()

    companion object {
        const val MAX_RECENT = 6
    }
}

/** Pure rule shared by the store and its tests. */
fun List<String>.withRecent(query: String): List<String> {
    val q = query.trim()
    if (q.isEmpty()) return this
    return (listOf(q) + filterNot { it.equals(q, ignoreCase = true) }).take(SettingsStore.MAX_RECENT)
}
