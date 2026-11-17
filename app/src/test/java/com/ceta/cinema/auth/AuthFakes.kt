package com.ceta.cinema.auth

import android.app.Activity
import android.content.Context
import com.ceta.cinema.data.remote.firebase.FirebaseAuthSource
import com.ceta.cinema.data.remote.firebase.PendingEmailStore
import com.ceta.cinema.data.remote.firebase.PhoneCodeResult

/** Firebase stand-in: set [failWith] to make the next call throw; calls are recorded in [calls]. */
class FakeFirebaseAuth(var idToken: String = "firebase-id-token") : FirebaseAuthSource {
    var failWith: Throwable? = null
    var phoneResult: PhoneCodeResult = PhoneCodeResult.CodeSent("verification-1")
    val calls = mutableListOf<String>()
    var signedOut = 0

    private fun hit(name: String): String { calls += name; failWith?.let { throw it }; return idToken }

    override suspend fun signInWithEmail(email: String, password: String) = hit("email:$email")
    override suspend fun signInWithGoogle(context: Context, webClientId: String) = hit("google")
    override suspend fun signInWithLink(email: String, link: String) = hit("link:$email")
    override fun isSignInLink(link: String) = link.contains("oobCode")
    override suspend fun signUp(email: String, password: String, displayName: String) { hit("signUp:$email:$displayName") }
    override suspend fun sendSignInLink(email: String) { hit("sendLink:$email") }
    override suspend fun sendPasswordReset(email: String) { hit("reset:$email") }
    override suspend fun sendPhoneCode(activity: Activity, phoneE164: String): PhoneCodeResult { hit("phone:$phoneE164"); return phoneResult }
    override suspend fun signInWithPhoneCode(verificationId: String, code: String) = hit("code:$verificationId:$code")
    override fun signOut() { signedOut++ }
    var method: com.ceta.cinema.data.remote.firebase.SignInMethod? = com.ceta.cinema.data.remote.firebase.SignInMethod.PASSWORD
    var phone: String? = null
    var deletedUser = 0
    override fun signInMethod() = method
    override fun phoneNumber() = phone
    override suspend fun deleteCurrentUser() { hit("deleteUser"); deletedUser++ }
}

class FakePendingEmail(var value: String? = null) : PendingEmailStore {
    override fun get() = value
    override fun set(email: String) { value = email }
    override fun clear() { value = null }
}
