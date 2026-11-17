package com.ceta.cinema.ui.screen.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.local.prefs.SettingsStore
import com.ceta.cinema.data.repository.AccountRepository
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.domain.model.Profile
import com.ceta.cinema.domain.model.Promotion
import com.ceta.cinema.domain.model.ReminderMovie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileMessage {
    data object Saved : ProfileMessage
    data object NameInvalid : ProfileMessage
    data object ResetSent : ProfileMessage
    data class Failed(val error: ApiError) : ProfileMessage
}

data class ProfileUi(val profile: Profile, val saving: Boolean = false, val message: ProfileMessage? = null) {
    /** Only an account with a real email has a password to reset. */
    val canResetPassword: Boolean get() = !profile.email.isNullOrBlank()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val account: AccountRepository,
    private val session: SessionRepository,
    private val settings: SettingsStore,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<ProfileUi>>(UiState.Loading)
    val state: StateFlow<UiState<ProfileUi>> = _state

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = account.profile()) {
                is ApiResult.Ok -> UiState.Content(ProfileUi(r.data))
                is ApiResult.Err -> UiState.Error(r.error)
            }
        }
    }

    fun saveName(name: String) {
        val ui = current() ?: return
        val clean = name.trim()
        if (clean.length !in NAME_LENGTH) { update { it.copy(message = ProfileMessage.NameInvalid) }; return }
        if (clean == ui.profile.username) return
        update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            when (val r = account.rename(clean)) {
                is ApiResult.Ok -> {
                    settings.setDisplayName(r.data.username)
                    update { it.copy(profile = r.data, saving = false, message = ProfileMessage.Saved) }
                }
                is ApiResult.Err -> update { it.copy(saving = false, message = ProfileMessage.Failed(r.error)) }
            }
        }
    }

    fun sendPasswordReset() {
        val email = current()?.profile?.email ?: return
        update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            val r = session.forgotPassword(email)
            update { it.copy(saving = false, message = if (r is ApiResult.Err) ProfileMessage.Failed(r.error) else ProfileMessage.ResetSent) }
        }
    }

    fun consumeMessage() = update { it.copy(message = null) }

    private fun current(): ProfileUi? = (_state.value as? UiState.Content)?.data
    private fun update(change: (ProfileUi) -> ProfileUi) { _state.update { s -> if (s is UiState.Content) UiState.Content(change(s.data)) else s } }

    companion object { val NAME_LENGTH = 3..50 }
}

/** The offers the customer saved to their wallet, with a way to drop one. */
@HiltViewModel
class VouchersViewModel @Inject constructor(private val account: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Promotion>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Promotion>>> = _state

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = account.wallet()) {
                is ApiResult.Err -> UiState.Error(r.error)
                is ApiResult.Ok -> if (r.data.isEmpty()) UiState.Empty() else UiState.Content(r.data)
            }
        }
    }

    /** Gone from the list at once; if the server refuses, the list is reloaded so it tells the truth. */
    fun remove(promotionId: Long) {
        val before = (_state.value as? UiState.Content)?.data ?: return
        val after = before.filterNot { it.id == promotionId }
        _state.value = if (after.isEmpty()) UiState.Empty() else UiState.Content(after)
        viewModelScope.launch { if (account.removeVoucher(promotionId) is ApiResult.Err) load() }
    }
}

/** Coming-soon movies the customer asked to be told about. */
@HiltViewModel
class RemindersViewModel @Inject constructor(private val account: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<ReminderMovie>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ReminderMovie>>> = _state

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val r = account.reminders()) {
                is ApiResult.Err -> UiState.Error(r.error)
                is ApiResult.Ok -> if (r.data.isEmpty()) UiState.Empty() else UiState.Content(r.data)
            }
        }
    }

    fun remove(movieId: String) {
        val before = (_state.value as? UiState.Content)?.data ?: return
        val after = before.filterNot { it.id == movieId }
        _state.value = if (after.isEmpty()) UiState.Empty() else UiState.Content(after)
        viewModelScope.launch { if (account.removeReminder(movieId) is ApiResult.Err) load() }
    }
}

enum class DeleteStep { IDLE, WARN, CONFIRM }

/**
 * Deleting the account: a warning, then the customer signs in once more (the way they normally do), and that fresh
 * sign-in is what the server accepts as proof. When it worked the session is gone; [deleted] tells the screen to leave.
 */
data class DeleteUi(
    val step: DeleteStep = DeleteStep.IDLE,
    val method: com.ceta.cinema.data.remote.firebase.SignInMethod? = null,
    val working: Boolean = false,
    val codeSentFor: String? = null,       // the verification id once an SMS code is on its way
    val error: ApiError? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(private val session: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow(DeleteUi())
    val state: StateFlow<DeleteUi> = _state

    fun start() { _state.value = DeleteUi(step = DeleteStep.WARN, method = session.signInMethod()) }
    fun cancel() { _state.value = DeleteUi() }
    fun proceed() { _state.update { it.copy(step = DeleteStep.CONFIRM, error = null) } }

    fun confirmWithPassword(email: String, password: String) = prove { session.idTokenByPassword(email, password) }
    fun confirmWithGoogle(context: android.content.Context, webClientId: String) = prove { session.idTokenByGoogle(context, webClientId) }
    fun confirmWithCode(code: String) {
        val id = _state.value.codeSentFor ?: return
        prove { session.idTokenByPhoneCode(id, code) }
    }

    fun sendCode(activity: android.app.Activity) {
        val phone = session.phoneNumber() ?: return
        _state.update { it.copy(working = true, error = null) }
        viewModelScope.launch {
            when (val r = session.startPhoneProof(activity, phone)) {
                is ApiResult.Err -> _state.update { it.copy(working = false, error = r.error) }
                is ApiResult.Ok -> when (val proof = r.data) {
                    is SessionRepository.PhoneProof.CodeSent -> _state.update { it.copy(working = false, codeSentFor = proof.verificationId) }
                    is SessionRepository.PhoneProof.Verified -> prove { ApiResult.Ok(proof.idToken) }
                }
            }
        }
    }

    private fun prove(getToken: suspend () -> ApiResult<String>) {
        _state.update { it.copy(working = true, error = null) }
        viewModelScope.launch {
            when (val token = getToken()) {
                is ApiResult.Err -> _state.update { it.copy(working = false, error = token.error) }
                is ApiResult.Ok -> when (val r = session.deleteAccount(token.data)) {
                    is ApiResult.Ok -> _state.update { it.copy(working = false, deleted = true) }
                    is ApiResult.Err -> _state.update { it.copy(working = false, error = r.error) }
                }
            }
        }
    }
}
