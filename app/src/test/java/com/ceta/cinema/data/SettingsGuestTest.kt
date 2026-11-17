package com.ceta.cinema.data

import com.ceta.cinema.core.AuthCodes
import com.ceta.cinema.core.toAuthError
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SettingsGuestTest {
    @Test fun guestChoiceIsRemembered() = runBlocking {
        val s = FakeSettingsStore()
        assertFalse(s.current.guestMode)
        s.setGuestMode(true); assertTrue(s.current.guestMode)
        s.setGuestMode(false); assertFalse(s.current.guestMode)
    }

    @Test fun aDisabledPhoneProviderGetsItsOwnMessage() {
        assertEquals(AuthCodes.PHONE_UNAVAILABLE, FirebaseAuthException("ERROR_OPERATION_NOT_ALLOWED", "x").toAuthError().code)
    }
}
