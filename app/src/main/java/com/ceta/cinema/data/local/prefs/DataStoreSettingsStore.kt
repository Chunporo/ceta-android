package com.ceta.cinema.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

    override val settings: Flow<AppSettings> = context.settingsDataStore.data.map {
        AppSettings(
            dark = it[dark] ?: true,
            language = it[language] ?: "vi",
            onboardingSeen = it[onboarding] ?: false,
            recentSearches = (it[recent] ?: "").split('\n').filter { s -> s.isNotBlank() },
        )
    }

    override suspend fun setDark(dark: Boolean) { context.settingsDataStore.edit { it[this.dark] = dark } }
    override suspend fun setLanguage(language: String) { context.settingsDataStore.edit { it[this.language] = language } }
    override suspend fun setOnboardingSeen() { context.settingsDataStore.edit { it[onboarding] = true } }

    override suspend fun addRecentSearch(query: String) {
        val current = settings.first().recentSearches
        context.settingsDataStore.edit { it[recent] = current.withRecent(query).joinToString("\n") }
    }

    override suspend fun clearRecentSearches() { context.settingsDataStore.edit { it.remove(recent) } }
}
