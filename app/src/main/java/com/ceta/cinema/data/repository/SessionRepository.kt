package com.ceta.cinema.data.repository

import android.content.Context
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.AuthCodes
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.toAuthError
import com.ceta.cinema.data.local.secure.TokenStore
import com.ceta.cinema.data.remote.api.CetaApi
import com.ceta.cinema.data.remote.dto.FirebaseLoginRequest
import com.ceta.cinema.data.remote.dto.RefreshRequest
import com.ceta.cinema.data.remote.firebase.FirebaseAuthSource
import com.ceta.cinema.data.remote.firebase.PendingEmailStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signs in with Firebase, then trades the Firebase ID token for the backend's own tokens at /auth/firebase.
 * [signedIn] is the single truth the navigation follows: null until the stored tokens have been read.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val api: CetaApi,
    private val tokens: TokenStore,
    private val firebase: FirebaseAuthSource,
    private val pendingEmail: PendingEmailStore,
) {
    private val _signedIn = MutableStateFlow<Boolean?>(null)
    val signedIn: StateFlow<Boolean?> = _signedIn

    suspend fun restore() {
        _signedIn.value = tokens.access() != null
    }

    suspend fun signInWithEmail(email: String, password: String): ApiResult<Unit> =
        exchange { firebase.signInWithEmail(email.trim(), password) }

    suspend fun signInWithGoogle(context: Context, webClientId: String): ApiResult<Unit> =
        exchange { firebase.signInWithGoogle(context, webClientId) }

    suspend fun sendEmailLink(email: String): ApiResult<Unit> = attempt {
        firebase.sendSignInLink(email.trim())
        pendingEmail.set(email.trim())
    }

    /** Called with the URL the app was opened from. Returns null when the URL is not a sign-in link. */
    suspend fun completeEmailLink(link: String): ApiResult<Unit>? {
        if (!firebase.isSignInLink(link)) return null
        // Opened on another device than the one that asked for the link: there is no email to match it with.
        val email = pendingEmail.get() ?: return ApiResult.Err(ApiError(AuthCodes.LINK_INVALID, 0))
        val result = exchange { firebase.signInWithLink(email, link) }
        if (result is ApiResult.Ok) pendingEmail.clear()
        return result
    }

    /** Creates the account and mails a verification link; the user signs in once the address is verified. */
    suspend fun register(username: String, email: String, password: String): ApiResult<Unit> =
        attempt { firebase.signUp(email.trim(), password, username.trim()) }

    /** The screen says the same thing whether or not the address exists, so an unknown address counts as success. */
    suspend fun forgotPassword(email: String): ApiResult<Unit> {
        val result = attempt { firebase.sendPasswordReset(email.trim()) }
        return if (result is ApiResult.Err && result.error.code == AuthCodes.INVALID_CREDENTIALS) ApiResult.Ok(Unit) else result
    }

    /** Local sign-out always completes, even offline: the server-side revoke is best effort. */
    suspend fun signOut() {
        val refresh = tokens.refresh()
        tokens.clear()
        firebase.signOut()
        _signedIn.value = false
        if (refresh != null) runCatching { api.logout(RefreshRequest(refresh)) }
    }

    /** The refresh token was rejected: AuthInterceptor already cleared the tokens. */
    fun onSessionExpired() {
        firebase.signOut()
        _signedIn.value = false
    }

    private suspend fun exchange(idToken: suspend () -> String): ApiResult<Unit> = attempt {
        val response = api.loginWithFirebase(FirebaseLoginRequest(idToken()))
        tokens.saveTokens(response.accessToken, response.refreshToken)
        _signedIn.value = true
    }.also { if (it is ApiResult.Err) firebase.signOut() } // no half signed-in state if the backend said no

    private suspend fun attempt(block: suspend () -> Unit): ApiResult<Unit> = try {
        block()
        ApiResult.Ok(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        ApiResult.Err(e.toAuthError())
    }
}
