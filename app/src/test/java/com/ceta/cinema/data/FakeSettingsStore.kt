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
    override suspend fun setDisplayName(name: String?) { state.value = state.value.copy(displayName = name) }
    override suspend fun addRecentSearch(query: String) { state.value = state.value.copy(recentSearches = state.value.recentSearches.withRecent(query)) }
    override suspend fun clearRecentSearches() { state.value = state.value.copy(recentSearches = emptyList()) }
}
