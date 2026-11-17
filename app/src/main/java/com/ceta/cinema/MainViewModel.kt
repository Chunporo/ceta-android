package com.ceta.cinema

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.SessionEvents
import com.ceta.cinema.data.local.prefs.AppSettings
import com.ceta.cinema.data.local.prefs.SettingsStore
import com.ceta.cinema.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val session: SessionRepository,
    private val settingsStore: SettingsStore,
    events: SessionEvents,
) : ViewModel() {
    val signedIn: StateFlow<Boolean?> = session.signedIn

    /** Null until the stored settings have been read, so the first frame is never drawn in the wrong theme or language. */
    val settings: StateFlow<AppSettings?> = settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _linkError = MutableStateFlow<ApiError?>(null)
    val linkError: StateFlow<ApiError?> = _linkError

    init {
        viewModelScope.launch { session.restore() }
        viewModelScope.launch { events.expired.collect { session.onSessionExpired() } }
    }

    fun onLink(link: String) {
        viewModelScope.launch {
            _linkError.value = (session.completeEmailLink(link) as? ApiResult.Err)?.error
        }
    }

    fun signOut() {
        viewModelScope.launch { session.signOut() }
    }

    fun setDark(dark: Boolean) { viewModelScope.launch { settingsStore.setDark(dark) } }
    fun setLanguage(language: String) { viewModelScope.launch { settingsStore.setLanguage(language) } }
    fun finishOnboarding() { viewModelScope.launch { settingsStore.setOnboardingSeen() } }
}
