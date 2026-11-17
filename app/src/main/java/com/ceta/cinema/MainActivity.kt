package com.ceta.cinema

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ceta.cinema.navigation.CetaNavHost
import com.ceta.cinema.ui.Localized
import com.ceta.cinema.ui.theme.CetaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleLink(intent)
        setContent {
            val settings by viewModel.settings.collectAsState()
            val s = settings
            if (s != null) {
                Localized(s.language) { CetaTheme(dark = s.dark) { CetaNavHost(viewModel, s) } }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLink(intent)
    }

    private fun handleLink(intent: Intent) {
        intent.data?.toString()?.let(viewModel::onLink)
    }
}
