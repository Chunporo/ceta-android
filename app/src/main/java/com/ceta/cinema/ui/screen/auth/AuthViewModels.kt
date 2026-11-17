package com.ceta.cinema.ui.screen.auth

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.Validators
import com.ceta.cinema.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    val error: ApiError? = null,
    val linkSentTo: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state

    fun onEmail(value: String) = _state.update { it.copy(email = value, emailError = null, error = null, linkSentTo = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }

    fun signIn() {
        val s = _state.value
        if (s.loading) return
        val emailError = Validators.email(s.email)
        val passwordError = if (s.password.isEmpty()) com.ceta.cinema.R.string.validation_password_required else null
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        run { session.signInWithEmail(s.email, s.password) }
    }

    fun signInWithGoogle(context: Context, webClientId: String) {
        if (_state.value.loading) return
        run { session.signInWithGoogle(context, webClientId) }
    }

    fun sendLink() {
        val s = _state.value
        if (s.loading) return
        val emailError = Validators.email(s.email)
        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }
        run(onOk = { copy(linkSentTo = s.email.trim()) }) { session.sendEmailLink(s.email) }
    }

    /** Navigation reacts to the signed-in flag, so success only has to stop the spinner; the email stays on failure. */
    private fun run(onOk: LoginUiState.() -> LoginUiState = { this }, call: suspend () -> ApiResult<Unit>) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = call()) {
                is ApiResult.Ok -> _state.update { it.onOk().copy(loading = false) }
                is ApiResult.Err -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }
}

data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val loading: Boolean = false,
    @StringRes val usernameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmError: Int? = null,
    val error: ApiError? = null,
    val verificationSentTo: String? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state

    fun onUsername(v: String) = _state.update { it.copy(username = v, usernameError = null, error = null) }
    fun onEmail(v: String) = _state.update { it.copy(email = v, emailError = null, error = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, passwordError = null, error = null) }
    fun onConfirm(v: String) = _state.update { it.copy(confirm = v, confirmError = null, error = null) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val usernameError = Validators.username(s.username)
        val emailError = Validators.email(s.email)
        val passwordError = Validators.password(s.password)
        val confirmError = Validators.confirm(s.password, s.confirm)
        if (listOf(usernameError, emailError, passwordError, confirmError).any { it != null }) {
            _state.update {
                it.copy(usernameError = usernameError, emailError = emailError, passwordError = passwordError, confirmError = confirmError)
            }
            return // a form that does not validate never reaches Firebase
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = session.register(s.username, s.email, s.password)) {
                is ApiResult.Ok -> _state.update { it.copy(loading = false, verificationSentTo = s.email.trim()) }
                is ApiResult.Err -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }
}

data class ForgotPasswordUiState(
    val email: String = "",
    val loading: Boolean = false,
    @StringRes val emailError: Int? = null,
    val error: ApiError? = null,
    val done: Boolean = false,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state

    fun onEmail(v: String) = _state.update { it.copy(email = v, emailError = null, error = null) }

    fun submit() {
        val s = _state.value
        if (s.loading) return
        val emailError = Validators.email(s.email)
        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = session.forgotPassword(s.email)) {
                is ApiResult.Ok -> _state.update { it.copy(loading = false, done = true) }
                is ApiResult.Err -> _state.update { it.copy(loading = false, error = result.error) }
            }
        }
    }
}
