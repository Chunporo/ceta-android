package com.ceta.cinema.auth

import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.SessionEvents
import com.ceta.cinema.data.FakeTokenStore
import com.ceta.cinema.data.apiFor
import com.ceta.cinema.data.json
import com.ceta.cinema.data.repository.SessionRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionRepositoryTest {
    private lateinit var server: MockWebServer
    private val firebase = FakeFirebaseAuth()
    private val tokens = FakeTokenStore()
    private val pending = FakePendingEmail()
    private lateinit var repo: SessionRepository

    @Before fun start() {
        server = MockWebServer().also { it.start() }
        repo = SessionRepository(apiFor(server, tokens, SessionEvents()), tokens, firebase, pending)
    }
    @After fun stop() { server.shutdown() }

    private val tokenBody = """{"accessToken":"acc","refreshToken":"ref","tokenType":"Bearer","expiresInSeconds":900}"""

    @Test fun email_sign_in_trades_the_firebase_token_for_backend_tokens() = runTest {
        server.enqueue(json(tokenBody))

        val result = repo.signInWithEmail(" a@b.vn ", "secret123")

        assertTrue(result is ApiResult.Ok)
        assertEquals("acc", tokens.accessToken)
        assertEquals("ref", tokens.refreshToken)
        assertEquals(true, repo.signedIn.value)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/firebase", request.path)
        assertNull("the exchange itself carries no bearer", request.getHeader("Authorization"))
        assertTrue(request.body.readUtf8().contains("firebase-id-token"))
        assertEquals(listOf("email:a@b.vn"), firebase.calls)
    }

    @Test fun wrong_password_reports_invalid_credentials_and_never_calls_the_backend() = runTest {
        firebase.failWith = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "bad")

        val error = (repo.signInWithEmail("a@b.vn", "wrong-pass") as ApiResult.Err).error

        assertEquals("AUTH_INVALID_CREDENTIALS", error.code)
        assertEquals(0, server.requestCount)
        assertNull(tokens.accessToken)
    }

    @Test fun an_unknown_account_looks_the_same_as_a_wrong_password() = runTest {
        firebase.failWith = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "none")
        assertEquals("AUTH_INVALID_CREDENTIALS", (repo.signInWithEmail("x@y.vn", "secret123") as ApiResult.Err).error.code)
    }

    @Test fun unverified_email_is_refused_by_the_backend_and_leaves_nothing_signed_in() = runTest {
        server.enqueue(json("""{"code":"EMAIL_NOT_VERIFIED","message":"Verify your email address before signing in","details":[]}""", 403))

        val error = (repo.signInWithEmail("a@b.vn", "secret123") as ApiResult.Err).error

        assertEquals("EMAIL_NOT_VERIFIED", error.code)
        assertNull(tokens.accessToken)
        assertNotEquals(true, repo.signedIn.value)
        assertEquals("Firebase session dropped so there is no half signed-in state", 1, firebase.signedOut)
    }

    @Test fun google_sign_in_uses_the_same_exchange() = runTest {
        server.enqueue(json(tokenBody))
        assertTrue(repo.signInWithGoogle(android.content.ContextWrapper(null), "web-client") is ApiResult.Ok)
        assertEquals(listOf("google"), firebase.calls)
        assertEquals("acc", tokens.accessToken)
    }

    @Test fun offline_sign_in_is_a_network_error() = runTest {
        firebase.failWith = FirebaseNetworkException("offline")
        assertEquals("NETWORK", (repo.signInWithEmail("a@b.vn", "secret123") as ApiResult.Err).error.code)
    }

    @Test fun email_link_remembers_the_address_then_completes_the_sign_in() = runTest {
        assertTrue(repo.sendEmailLink(" a@b.vn ") is ApiResult.Ok)
        assertEquals("a@b.vn", pending.value)

        server.enqueue(json(tokenBody))
        val result = repo.completeEmailLink("https://ceta-dedff.firebaseapp.com/finishSignIn?oobCode=abc")

        assertTrue(result is ApiResult.Ok)
        assertEquals("link:a@b.vn", firebase.calls.last())
        assertNull("the pending address is used once", pending.value)
        assertEquals("acc", tokens.accessToken)
    }

    @Test fun a_link_opened_without_a_pending_email_is_rejected() = runTest {
        val result = repo.completeEmailLink("https://ceta-dedff.firebaseapp.com/finishSignIn?oobCode=abc")
        assertEquals("AUTH_LINK_INVALID", (result as ApiResult.Err).error.code)
        assertEquals(0, server.requestCount)
    }

    @Test fun a_url_that_is_not_a_sign_in_link_is_ignored() = runTest {
        assertNull(repo.completeEmailLink("https://example.com/whatever"))
    }

    @Test fun register_creates_the_account_without_signing_in() = runTest {
        assertTrue(repo.register(" An ", "a@b.vn", "secret123") is ApiResult.Ok)
        assertEquals(listOf("signUp:a@b.vn:An"), firebase.calls)
        assertEquals(0, server.requestCount)
        assertNull(tokens.accessToken)
    }

    @Test fun register_with_a_taken_email_says_so() = runTest {
        firebase.failWith = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "taken")
        assertEquals("AUTH_EMAIL_IN_USE", (repo.register("An", "a@b.vn", "secret123") as ApiResult.Err).error.code)
    }

    @Test fun forgot_password_does_not_reveal_whether_the_email_exists() = runTest {
        firebase.failWith = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "none")
        assertTrue(repo.forgotPassword("nobody@b.vn") is ApiResult.Ok)
    }

    @Test fun forgot_password_still_reports_a_network_failure() = runTest {
        firebase.failWith = FirebaseNetworkException("offline")
        assertEquals("NETWORK", (repo.forgotPassword("a@b.vn") as ApiResult.Err).error.code)
    }

    @Test fun sign_out_revokes_on_the_server_and_clears_everything_locally() = runTest {
        tokens.saveTokens("acc", "ref")
        server.enqueue(MockResponse().setResponseCode(204))

        repo.signOut()

        assertNull(tokens.accessToken)
        assertEquals(false, repo.signedIn.value)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/logout", request.path)
        assertTrue(request.body.readUtf8().contains("ref"))
    }

    @Test fun sign_out_completes_even_when_the_server_is_unreachable() = runTest {
        tokens.saveTokens("acc", "ref")
        server.shutdown()

        repo.signOut()

        assertNull(tokens.accessToken)
        assertEquals(false, repo.signedIn.value)
    }

    @Test fun restore_reads_the_stored_tokens() = runTest {
        assertNull(repo.signedIn.value)
        repo.restore()
        assertEquals(false, repo.signedIn.value)
        tokens.saveTokens("acc", "ref")
        repo.restore()
        assertEquals(true, repo.signedIn.value)
    }

    @Test fun an_expired_session_flips_the_flag_and_drops_firebase() = runTest {
        tokens.saveTokens("acc", "ref"); repo.restore()
        repo.onSessionExpired()
        assertEquals(false, repo.signedIn.value)
        assertEquals(1, firebase.signedOut)
    }
}
