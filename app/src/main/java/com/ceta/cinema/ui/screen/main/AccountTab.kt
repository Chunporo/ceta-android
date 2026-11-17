package com.ceta.cinema.ui.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ceta.cinema.R
import com.ceta.cinema.data.local.prefs.AppSettings
import com.ceta.cinema.ui.component.OutlineButton
import com.ceta.cinema.ui.component.PrimaryButton
import com.ceta.cinema.ui.component.SegmentedTabs
import com.ceta.cinema.ui.theme.Ceta

/** Settings that exist without an account: look and language, plus sign in or out. The profile block arrives with the loyalty data. */
@Composable
fun AccountTab(
    settings: AppSettings, signedIn: Boolean,
    onDark: (Boolean) -> Unit, onLanguage: (String) -> Unit, onSignIn: () -> Unit, onSignOut: () -> Unit,
) {
    val c = Ceta.colors
    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.ds_nav_account), style = MaterialTheme.typography.titleLarge, color = c.tx)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ds_dark_mode), color = c.tx, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = settings.dark, onCheckedChange = onDark,
                colors = SwitchDefaults.colors(checkedTrackColor = c.ac, checkedThumbColor = androidx.compose.ui.graphics.Color.White))
        }
        Text(stringResource(R.string.ds_lang), color = c.mu, style = MaterialTheme.typography.labelMedium)
        SegmentedTabs(listOf("VI", "EN"), if (settings.language == "en") 1 else 0, { onLanguage(if (it == 1) "en" else "vi") })
        Spacer(Modifier.height(8.dp))
        if (signedIn) OutlineButton(stringResource(R.string.ds_logout), onSignOut) else PrimaryButton(stringResource(R.string.ds_login), onSignIn)
    }
}
