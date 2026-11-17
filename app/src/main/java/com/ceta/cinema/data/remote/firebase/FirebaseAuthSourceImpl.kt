package com.ceta.cinema.data.remote.firebase

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.ceta.cinema.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.actionCodeSettings
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.firestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthSourceImpl @Inject constructor() : FirebaseAuthSource {
    private val auth get() = Firebase.auth

    override suspend fun signInWithEmail(email: String, password: String): String =
        finish(auth.signInWithEmailAndPassword(email, password).await().user!!)

    override suspend fun signInWithGoogle(context: Context, webClientId: String): String {
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val credential = CredentialManager.create(context)
            .getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
            .credential
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Unexpected credential type"
        }
        val googleToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        return finish(auth.signInWithCredential(GoogleAuthProvider.getCredential(googleToken, null)).await().user!!)
    }

    override suspend fun signInWithLink(email: String, link: String): String =
        finish(auth.signInWithEmailLink(email, link).await().user!!)

    override fun isSignInLink(link: String): Boolean = auth.isSignInWithEmailLink(link)

    override suspend fun signUp(email: String, password: String, displayName: String) {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user!!
        user.updateProfile(userProfileChangeRequest { this.displayName = displayName }).await()
        user.sendEmailVerification().await()
        // The backend will not accept this account until the address is verified: leave no half-signed-in state.
        auth.signOut()
    }

    override suspend fun sendSignInLink(email: String) {
        val settings = actionCodeSettings {
            url = LINK_URL
            handleCodeInApp = true
            setAndroidPackageName(BuildConfig.APPLICATION_ID, true, null)
        }
        auth.sendSignInLinkToEmail(email, settings).await()
    }

    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    override fun signOut() = auth.signOut()

    /** A forced refresh, so a freshly verified email shows up as email_verified in the token. */
    private suspend fun finish(user: FirebaseUser): String {
        runCatching {
            Firebase.firestore.collection("users").document(user.uid)
                .set(mapOf("email" to user.email, "displayName" to user.displayName)).await()
        }
        return user.getIdToken(true).await().token!!
    }

    private companion object {
        const val LINK_URL = "https://ceta-dedff.firebaseapp.com/finishSignIn"
    }
}

@Singleton
class PrefsPendingEmailStore @Inject constructor(@ApplicationContext context: Context) : PendingEmailStore {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    override fun get(): String? = prefs.getString("pending_email", null)
    override fun set(email: String) { prefs.edit().putString("pending_email", email).apply() }
    override fun clear() { prefs.edit().remove("pending_email").apply() }
}
