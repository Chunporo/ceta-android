package com.ceta.cinema.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ceta.cinema.R
import com.ceta.cinema.ui.theme.Spacing

/** D1 placeholder. D3 replaces it with the real form, driven by a ViewModel and one UiState. */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(Spacing.m),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.titleLarge)
        Button(onClick = onLoggedIn, modifier = Modifier.padding(top = Spacing.l)) {
            Text(stringResource(R.string.login_action))
        }
    }
}
