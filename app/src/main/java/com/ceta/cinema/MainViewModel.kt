package com.ceta.cinema

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.SessionEvents
import com.ceta.cinema.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val session: SessionRepository,
    events: SessionEvents,
) : ViewModel() {
    val signedIn: StateFlow<Boolean?> = session.signedIn

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
}
