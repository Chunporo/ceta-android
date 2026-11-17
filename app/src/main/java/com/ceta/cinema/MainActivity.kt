package com.ceta.cinema

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.lifecycle.lifecycleScope
import com.ceta.cinema.auth.AuthRepository
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val auth = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleSignInLink(intent)
        setContent { MaterialTheme { Text("CETA Cinema") } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSignInLink(intent)
    }

    /** Completes email-link sign-in when the app is opened from the emailed link. */
    private fun handleSignInLink(intent: Intent) {
        val link = intent.data?.toString() ?: return
        if (!auth.isSignInLink(link)) return
        // The login screen should save the typed email under "pending_email" when it sends the link.
        val email = getSharedPreferences("auth", MODE_PRIVATE).getString("pending_email", null) ?: return
        lifecycleScope.launch {
            runCatching { auth.signInWithLink(email, link) }
        }
    }
}
