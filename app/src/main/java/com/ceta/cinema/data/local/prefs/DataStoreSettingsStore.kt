package com.ceta.cinema.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class DataStoreSettingsStore @Inject constructor(@ApplicationContext private val context: Context) : SettingsStore {
    private val dark = booleanPreferencesKey("dark")
    private val language = stringPreferencesKey("language")
    private val onboarding = booleanPreferencesKey("onboarding_seen")
    private val recent = stringPreferencesKey("recent_searches")
    private val name = stringPreferencesKey("display_name")
    private val pendingId = longPreferencesKey("pending_booking_id")
    private val pendingUntil = longPreferencesKey("pending_hold_until")

    override val settings: Flow<AppSettings> = context.settingsDataStore.data.map {
        AppSettings(
            dark = it[dark] ?: true,
            language = it[language] ?: "vi",
            onboardingSeen = it[onboarding] ?: false,
            recentSearches = (it[recent] ?: "").split('\n').filter { s -> s.isNotBlank() },
            displayName = it[name],
            pendingBookingId = it[pendingId],
            pendingHoldUntilMillis = it[pendingUntil] ?: 0,
        )
    }

    override suspend fun setDark(dark: Boolean) { context.settingsDataStore.edit { it[this.dark] = dark } }
    override suspend fun setLanguage(language: String) { context.settingsDataStore.edit { it[this.language] = language } }
    override suspend fun setOnboardingSeen() { context.settingsDataStore.edit { it[onboarding] = true } }

    override suspend fun setDisplayName(name: String?) {
        context.settingsDataStore.edit { if (name.isNullOrBlank()) it.remove(this.name) else it[this.name] = name }
    }

    override suspend fun setPendingBooking(bookingId: Long, holdUntilMillis: Long) {
        context.settingsDataStore.edit { it[pendingId] = bookingId; it[pendingUntil] = holdUntilMillis }
    }

    override suspend fun clearPendingBooking() {
        context.settingsDataStore.edit { it.remove(pendingId); it.remove(pendingUntil) }
    }

    override suspend fun addRecentSearch(query: String) {
        val current = settings.first().recentSearches
        context.settingsDataStore.edit { it[recent] = current.withRecent(query).joinToString("\n") }
    }

    override suspend fun clearRecentSearches() { context.settingsDataStore.edit { it.remove(recent) } }
}
