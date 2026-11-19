package com.ceta.cinema.auth

import android.app.Activity
import com.ceta.cinema.R
import com.ceta.cinema.core.SessionEvents
import com.ceta.cinema.data.FakeTokenStore
import com.ceta.cinema.data.apiFor
import com.ceta.cinema.data.json
import com.ceta.cinema.data.remote.firebase.PhoneCodeResult
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.ui.screen.auth.LoginStep
import com.ceta.cinema.ui.screen.auth.PhoneLoginViewModel
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneLoginViewModelTest {
    private lateinit var server: MockWebServer
    private val firebase = FakeFirebaseAuth()
    private val tokens = FakeTokenStore()
    private lateinit var session: SessionRepository
    private val tokenBody = """{"accessToken":"acc","refreshToken":"ref"}"""

    @Before fun start() {
        server = MockWebServer().also { it.start() }
        session = SessionRepository(apiFor(server, tokens, SessionEvents()), tokens, firebase, FakePendingEmail(), com.ceta.cinema.data.FakeSettingsStore())
    }
    @After fun stop() { server.shutdown() } // Main is left set: a ViewModel from an earlier test may still have a coroutine in flight

    /** The backend exchange runs on a real OkHttp thread, so wait for its effect instead of asserting straight away. */
    private fun eventually(timeoutMs: Long = 3_000, check: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!check() && System.currentTimeMillis() < end) Thread.sleep(10)
        assertTrue("condition not met within ${timeoutMs}ms", check())
    }

    private fun TestScope.vm(): PhoneLoginViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return PhoneLoginViewModel(session)
    }

    @Test fun anInvalidNumberNeverReachesFirebase() = runTest {
        val vm = vm()
        vm.onPhone("12345")
        vm.sendCode(Activity())
        assertEquals(R.string.ds_phone_err, vm.state.value.phoneError)
        assertTrue(firebase.calls.isEmpty())
        assertEquals(LoginStep.PHONE, vm.state.value.step)
    }

    @Test fun aValidNumberSendsTheCodeInE164AndMovesToTheCodeStep() = runTest {
        val vm = vm()
        vm.onPhone("0912 345 678")
        vm.sendCode(Activity())

        val s = vm.state.value
        assertEquals(listOf("phone:+84912345678"), firebase.calls)
        assertEquals(LoginStep.OTP, s.step)
        assertEquals("verification-1", s.verificationId)
        assertEquals("+84912345678", s.sentTo)
        assertEquals(PhoneLoginViewModel.RESEND_SECONDS, s.resendSeconds)
        assertFalse(s.loading)
    }

    @Test fun theResendCountdownTicksDownToZeroAndStops() = runTest {
        val vm = vm()
        vm.onPhone("912345678")
        vm.sendCode(Activity())

        advanceTimeBy(1_001)
        assertEquals(59, vm.state.value.resendSeconds)
        advanceTimeBy(60_000)
        assertEquals(0, vm.state.value.resendSeconds)
        advanceTimeBy(5_000)
        assertEquals("never negative", 0, vm.state.value.resendSeconds)
    }

    @Test fun codeAndPhoneInputIsCleanedAndLimited() = runTest {
        val vm = vm()
        vm.onCode("12a3-4567 89")
        assertEquals("123456", vm.state.value.code)
        vm.onPhone("09x1 2-3")
        assertFalse(vm.state.value.phone.contains('x'))
        assertFalse(vm.state.value.phone.contains('-'))
    }

    @Test fun aShortCodeIsRefusedWithoutACall() = runTest {
        val vm = vm()
        vm.onPhone("912345678"); vm.sendCode(Activity())
        firebase.calls.clear()
        vm.onCode("123")

        vm.verify()

        assertEquals(R.string.ds_otp_err, vm.state.value.codeError)
        assertTrue(firebase.calls.isEmpty())
    }

    @Test fun theRightCodeSignsInThroughTheBackendExchange() = runTest {
        val vm = vm()
        vm.onPhone("912345678"); vm.sendCode(Activity())
        server.enqueue(json(tokenBody))
        vm.onCode("123456")

        vm.verify()

        eventually { session.signedIn.value == true }
        assertEquals("code:verification-1:123456", firebase.calls.last())
        assertEquals("acc", tokens.accessToken)
        eventually { !vm.state.value.loading }
    }

    @Test fun aWrongCodeStaysOnTheCodeStepClearsTheBoxesAndSaysSo() = runTest {
        val vm = vm()
        vm.onPhone("912345678"); vm.sendCode(Activity())
        firebase.failWith = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_VERIFICATION_CODE", "bad")
        vm.onCode("000000")

        vm.verify()

        val s = vm.state.value
        assertEquals(LoginStep.OTP, s.step)
        assertEquals("AUTH_INVALID_CODE", s.error?.code)
        assertEquals("", s.code)
        assertNull(tokens.accessToken)
    }

    @Test fun anExpiredCodeIsToldApart() = runTest {
        val vm = vm()
        vm.onPhone("912345678"); vm.sendCode(Activity())
        firebase.failWith = FirebaseAuthInvalidCredentialsException("ERROR_SESSION_EXPIRED", "old")
        vm.onCode("123456"); vm.verify()
        assertEquals("AUTH_CODE_EXPIRED", vm.state.value.error?.code)
    }

    @Test fun aPhoneThatVerifiesItselfSkipsTheCodeStep() = runTest {
        firebase.phoneResult = PhoneCodeResult.AutoVerified("firebase-id-token")
        server.enqueue(json(tokenBody))
        val vm = vm()
        vm.onPhone("912345678")

        vm.sendCode(Activity())

        eventually { session.signedIn.value == true }
        assertEquals(LoginStep.PHONE, vm.state.value.step)
        assertEquals("acc", tokens.accessToken)
    }

    @Test fun firebaseRefusingTheNumberIsShownAndTheScreenStaysPut() = runTest {
        firebase.failWith = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_PHONE_NUMBER", "bad")
        val vm = vm()
        vm.onPhone("912345678"); vm.sendCode(Activity())
        assertEquals("AUTH_INVALID_PHONE", vm.state.value.error?.code)
        assertEquals(LoginStep.PHONE, vm.state.value.step)
    }

    @Test fun backReturnsToTheNumberAndKeepsWhatWasTyped() = runTest {
        val vm = vm()
        vm.onPhone("912345678"); vm.sendCode(Activity())
        vm.onCode("123")

        vm.back()

        val s = vm.state.value
        assertEquals(LoginStep.PHONE, s.step)
        assertEquals("912345678", s.phone)
        assertEquals("", s.code)
        assertEquals(0, s.resendSeconds)
        advanceTimeBy(5_000)
        assertEquals("the cancelled countdown does not keep running", 0, vm.state.value.resendSeconds)
    }

    @Test fun googleSignInUsesTheSameExchange() = runTest {
        server.enqueue(json(tokenBody))
        val vm = vm()
        vm.signInWithGoogle(android.content.ContextWrapper(null), "web-client")
        eventually { session.signedIn.value == true }
        assertEquals("google", firebase.calls.single())
    }

    @Test fun cancellingTheGooglePickerShowsACalmMessage() = runTest {
        firebase.failWith = androidx.credentials.exceptions.GetCredentialCancellationException("cancel")
        val vm = vm()
        vm.signInWithGoogle(android.content.ContextWrapper(null), "web-client")
        assertEquals("AUTH_CANCELLED", vm.state.value.error?.code)
    }
}
