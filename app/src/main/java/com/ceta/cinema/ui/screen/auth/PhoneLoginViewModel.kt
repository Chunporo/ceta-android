package com.ceta.cinema.ui.screen.auth

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.R
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.PhoneNumber
import com.ceta.cinema.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoginStep { PHONE, OTP }

data class PhoneLoginUiState(
    val phone: String = "",
    val step: LoginStep = LoginStep.PHONE,
    val verificationId: String? = null,
    val sentTo: String? = null,
    val code: String = "",
    val loading: Boolean = false,
    @StringRes val phoneError: Int? = null,
    @StringRes val codeError: Int? = null,
    val error: ApiError? = null,
    val resendSeconds: Int = 0,
)

/** The design's login: a phone number, a 6-digit SMS code, or Google. Signing in flips the session flag; navigation follows it. */
@HiltViewModel
class PhoneLoginViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow(PhoneLoginUiState())
    val state: StateFlow<PhoneLoginUiState> = _state
    private var countdown: Job? = null

    fun onPhone(value: String) = _state.update { it.copy(phone = value.filter { c -> c.isDigit() || c == ' ' || c == '+' }.take(16), phoneError = null, error = null) }

    fun onCode(value: String) = _state.update { it.copy(code = value.filter(Char::isDigit).take(CODE_LENGTH), codeError = null, error = null) }

    fun sendCode(activity: Activity) {
        val s = _state.value
        if (s.loading) return
        val e164 = PhoneNumber.normalize(s.phone)
        if (e164 == null) {
            _state.update { it.copy(phoneError = R.string.ds_phone_err) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = session.sendPhoneCode(activity, e164)) {
                is ApiResult.Ok -> when (val start = result.data) {
                    is SessionRepository.PhoneStart.CodeSent -> {
                        _state.update { it.copy(loading = false, step = LoginStep.OTP, verificationId = start.verificationId, sentTo = e164, code = "") }
                        startCountdown()
                    }
                    SessionRepository.PhoneStart.SignedIn -> _state.update { it.copy(loading = false) }
                }
                is ApiResult.Err -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    fun verify() {
        val s = _state.value
        if (s.loading) return
        val id = s.verificationId ?: return
        if (s.code.length != CODE_LENGTH) {
            _state.update { it.copy(codeError = R.string.ds_otp_err) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = session.confirmPhoneCode(id, s.code)) {
                is ApiResult.Ok -> _state.update { it.copy(loading = false) }
                is ApiResult.Err -> _state.update { it.copy(loading = false, error = result.error, code = "") }
            }
        }
    }

    /** Back to the number, keeping what was typed. */
    fun back() {
        countdown?.cancel()
        _state.update { it.copy(step = LoginStep.PHONE, code = "", codeError = null, error = null, verificationId = null, resendSeconds = 0) }
    }

    fun signInWithGoogle(context: Context, webClientId: String) {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = session.signInWithGoogle(context, webClientId)) {
                is ApiResult.Ok -> _state.update { it.copy(loading = false) }
                is ApiResult.Err -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        _state.update { it.copy(resendSeconds = RESEND_SECONDS) }
        countdown = viewModelScope.launch {
            while (_state.value.resendSeconds > 0) {
                delay(1000)
                _state.update { it.copy(resendSeconds = it.resendSeconds - 1) }
            }
        }
    }

    companion object {
        const val CODE_LENGTH = 6
        const val RESEND_SECONDS = 60
    }
}
