package com.ceta.cinema.auth

import com.ceta.cinema.R
import com.ceta.cinema.core.SessionEvents
import com.ceta.cinema.core.Validators
import com.ceta.cinema.data.FakeTokenStore
import com.ceta.cinema.data.apiFor
import com.ceta.cinema.data.json
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.ui.screen.auth.ForgotPasswordViewModel
import com.ceta.cinema.ui.screen.auth.LoginViewModel
import com.ceta.cinema.ui.screen.auth.RegisterViewModel
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelsTest {
    private lateinit var server: MockWebServer
    private val firebase = FakeFirebaseAuth()
    private val tokens = FakeTokenStore()
    private lateinit var session: SessionRepository

    @Before fun start() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server = MockWebServer().also { it.start() }
        session = SessionRepository(apiFor(server, tokens, SessionEvents()), tokens, firebase, FakePendingEmail())
    }
    @After fun stop() { server.shutdown(); Dispatchers.resetMain() }

    @Test fun login_with_a_bad_form_makes_no_call_and_flags_the_fields() = runTest {
        val vm = LoginViewModel(session)
        vm.onEmail("not-an-email")
        vm.signIn()
        assertEquals(R.string.validation_email, vm.state.value.emailError)
        assertEquals(R.string.validation_password_required, vm.state.value.passwordError)
        assertTrue(firebase.calls.isEmpty())
    }

    @Test fun a_wrong_password_keeps_the_email_and_shows_the_error() = runTest {
        firebase.failWith = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "bad")
        val vm = LoginViewModel(session)
        vm.onEmail("a@b.vn"); vm.onPassword("wrong-pass")

        vm.signIn()

        val s = vm.state.value
        assertEquals("a@b.vn", s.email)
        assertEquals("AUTH_INVALID_CREDENTIALS", s.error?.code)
        assertFalse(s.loading)
    }

    @Test fun typing_clears_the_previous_error() = runTest {
        firebase.failWith = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "bad")
        val vm = LoginViewModel(session)
        vm.onEmail("a@b.vn"); vm.onPassword("wrong-pass"); vm.signIn()
        vm.onPassword("another-try")
        assertNull(vm.state.value.error)
    }

    @Test fun the_email_link_button_needs_a_valid_email_and_reports_where_it_went() = runTest {
        val vm = LoginViewModel(session)
        vm.sendLink()
        assertEquals(R.string.validation_email, vm.state.value.emailError)

        vm.onEmail("a@b.vn")
        vm.sendLink()
        assertEquals("a@b.vn", vm.state.value.linkSentTo)
    }

    @Test fun register_does_not_call_firebase_when_validation_fails() = runTest {
        val vm = RegisterViewModel(session)
        vm.onUsername("ab"); vm.onEmail("bad"); vm.onPassword("short"); vm.onConfirm("different")

        vm.submit()

        val s = vm.state.value
        assertEquals(R.string.validation_username, s.usernameError)
        assertEquals(R.string.validation_email, s.emailError)
        assertEquals(R.string.validation_password, s.passwordError)
        assertEquals(R.string.validation_confirm, s.confirmError)
        assertTrue("no Firebase call for an invalid form", firebase.calls.isEmpty())
    }

    @Test fun register_success_asks_the_user_to_verify_their_email() = runTest {
        val vm = RegisterViewModel(session)
        vm.onUsername("An Nguyen"); vm.onEmail("a@b.vn"); vm.onPassword("secret123"); vm.onConfirm("secret123")

        vm.submit()

        assertEquals("a@b.vn", vm.state.value.verificationSentTo)
        assertEquals(listOf("signUp:a@b.vn:An Nguyen"), firebase.calls)
    }

    @Test fun forgot_password_always_ends_on_the_neutral_message() = runTest {
        firebase.failWith = com.google.firebase.auth.FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "none")
        val vm = ForgotPasswordViewModel(session)
        vm.onEmail("nobody@b.vn")
        vm.submit()
        assertTrue(vm.state.value.done)
    }

    @Test fun validators() {
        assertNull(Validators.email("a@b.vn"))
        assertNotNull(Validators.email("a@b"))
        assertNull(Validators.password("12345678"))
        assertNotNull(Validators.password("1234567"))
        assertNull(Validators.username("abc"))
        assertNotNull(Validators.username("ab"))
        assertNull(Validators.confirm("x", "x"))
        assertNotNull(Validators.confirm("x", "y"))
    }
}
