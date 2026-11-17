package com.ceta.cinema.data.remote.firebase

import android.app.Activity
import android.content.Context

/** Firebase Authentication as the app uses it. Every sign-in returns a fresh ID token for the backend exchange. */
interface FirebaseAuthSource {
    suspend fun signInWithEmail(email: String, password: String): String
    suspend fun signInWithGoogle(context: Context, webClientId: String): String
    suspend fun signInWithLink(email: String, link: String): String
    fun isSignInLink(link: String): Boolean

    /** Creates the account, stores [displayName] (the backend derives the username from it) and mails a verification link. */
    suspend fun signUp(email: String, password: String, displayName: String)
    suspend fun sendSignInLink(email: String)
    suspend fun sendPasswordReset(email: String)
    /** Sends the SMS code. Some phones verify by themselves (instant verification or auto-retrieval): then there is no code to type. */
    suspend fun sendPhoneCode(activity: Activity, phoneE164: String): PhoneCodeResult

    suspend fun signInWithPhoneCode(verificationId: String, code: String): String
    fun signOut()
}

sealed interface PhoneCodeResult {
    data class CodeSent(val verificationId: String) : PhoneCodeResult
    data class AutoVerified(val idToken: String) : PhoneCodeResult
}

/** Where the typed email waits while the user reads the sign-in link in their mailbox. */
interface PendingEmailStore {
    fun get(): String?
    fun set(email: String)
    fun clear()
}
