package com.ceta.cinema.core

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.gson.JsonParseException
import java.io.IOException

object AuthCodes {
    const val INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS"
    const val EMAIL_IN_USE = "AUTH_EMAIL_IN_USE"
    const val WEAK_PASSWORD = "AUTH_WEAK_PASSWORD"
    const val CANCELLED = "AUTH_CANCELLED"
    const val LINK_INVALID = "AUTH_LINK_INVALID"
    const val INVALID_CODE = "AUTH_INVALID_CODE"
    const val CODE_EXPIRED = "AUTH_CODE_EXPIRED"
    const val INVALID_PHONE = "AUTH_INVALID_PHONE"
    const val PHONE_UNAVAILABLE = "AUTH_PHONE_UNAVAILABLE"
}

/** One mapping for everything a sign-in can throw, so screens only ever deal with [ApiError]. */
fun Throwable.toAuthError(): ApiError = when (this) {
    is ApiException -> error
    is FirebaseAuthWeakPasswordException -> ApiError(AuthCodes.WEAK_PASSWORD, 0)       // before its parent class
    is FirebaseAuthInvalidCredentialsException -> when (errorCode) {
        "ERROR_INVALID_VERIFICATION_CODE" -> ApiError(AuthCodes.INVALID_CODE, 0)
        "ERROR_SESSION_EXPIRED" -> ApiError(AuthCodes.CODE_EXPIRED, 0)
        "ERROR_INVALID_PHONE_NUMBER" -> ApiError(AuthCodes.INVALID_PHONE, 0)
        else -> ApiError(AuthCodes.INVALID_CREDENTIALS, 0)
    }
    is FirebaseAuthInvalidUserException -> ApiError(AuthCodes.INVALID_CREDENTIALS, 0)  // never reveal which part was wrong
    is FirebaseAuthUserCollisionException -> ApiError(AuthCodes.EMAIL_IN_USE, 0)
    is FirebaseAuthActionCodeException -> ApiError(AuthCodes.LINK_INVALID, 0)
    is FirebaseAuthException -> if (errorCode == "ERROR_OPERATION_NOT_ALLOWED" || errorCode == "ERROR_BILLING_NOT_ENABLED") ApiError(AuthCodes.PHONE_UNAVAILABLE, 0) else ApiError(ApiError.UNKNOWN, 0, message = errorCode)
    is FirebaseTooManyRequestsException -> ApiError("TOO_MANY_ATTEMPTS", 429)
    is FirebaseNetworkException -> ApiError(ApiError.NETWORK, 0)
    is GetCredentialCancellationException, is NoCredentialException -> ApiError(AuthCodes.CANCELLED, 0)
    is JsonParseException -> ApiError(ApiError.UNKNOWN, 0)
    is IOException -> ApiError(ApiError.NETWORK, 0)
    else -> ApiError(ApiError.UNKNOWN, 0, message = javaClass.simpleName)
}
