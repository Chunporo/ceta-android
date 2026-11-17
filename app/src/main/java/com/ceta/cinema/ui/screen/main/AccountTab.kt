package com.ceta.cinema.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.local.prefs.AppSettings
import com.ceta.cinema.domain.model.Loyalty
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.screen.account.AccountViewModel
import com.ceta.cinema.ui.theme.Ceta

@Composable
fun AccountTab(
    settings: AppSettings, signedIn: Boolean,
    onDark: (Boolean) -> Unit, onLanguage: (String) -> Unit, onSignIn: () -> Unit, onSignOut: () -> Unit,
    onHistory: () -> Unit = {}, onSoon: () -> Unit = {},
    vm: AccountViewModel = hiltViewModel(),
) {
    val c = Ceta.colors
    val loyalty by vm.loyalty.collectAsState()
    val name by vm.displayName.collectAsState()
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.ds_nav_account), style = MaterialTheme.typography.titleLarge, color = c.tx)
        if (signedIn) {
            Header(name, (loyalty as? UiState.Content)?.data)
            when (val l = loyalty) {
                UiState.Loading -> Box(Modifier.fillMaxWidth().height(96.dp)) { CircularProgressIndicator(Modifier.align(Alignment.Center), color = c.ac) }
                is UiState.Error -> ErrorBox(l.error, vm::load, Modifier.height(140.dp))
                is UiState.Empty -> Unit
                is UiState.Content -> { PointsCard(l.data); Stats(l.data) }
            }
            Menu(Icons.Rounded.Person, R.string.ds_profile, onSoon)
            Menu(Icons.Rounded.ReceiptLong, R.string.ds_history, onHistory)
            Menu(Icons.Rounded.CreditCard, R.string.ds_pay_methods, onSoon)
            Menu(Icons.Rounded.HelpOutline, R.string.ds_help, onSoon)
        } else {
            EmptyState(stringResource(R.string.account_guest_title), cta = stringResource(R.string.ds_login), onCta = onSignIn)
        }
        HorizontalDivider(color = c.ln2)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ds_dark_mode), color = c.tx, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = settings.dark, onCheckedChange = onDark, colors = SwitchDefaults.colors(checkedTrackColor = c.ac, checkedThumbColor = Color.White))
        }
        Text(stringResource(R.string.ds_lang), color = c.mu, style = MaterialTheme.typography.labelMedium)
        SegmentedTabs(listOf("VI", "EN"), if (settings.language == "en") 1 else 0, { onLanguage(if (it == 1) "en" else "vi") })
        if (signedIn) OutlineButton(stringResource(R.string.ds_logout), onSignOut)
    }
}

@Composable
private fun Header(name: String?, loyalty: Loyalty?) {
    val c = Ceta.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(c.ac), contentAlignment = Alignment.Center) {
            Text((name?.firstOrNull() ?: 'C').uppercase(), color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        Column {
            Text(name ?: "CETA", color = c.tx, style = MaterialTheme.typography.titleMedium)
            loyalty?.let { Text(stringResource(if (it.isTopTier) R.string.tier_vip else R.string.tier_member), color = c.ac, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun PointsCard(l: Loyalty) {
    val c = Ceta.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.sf).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(thousands(l.balance), color = c.tx, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.ds_points), color = c.mu, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 4.dp))
        }
        LinearProgressIndicator(progress = { l.progressToNextTier }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = c.ac, trackColor = c.sf2)
        Text(if (l.isTopTier) stringResource(R.string.top_tier) else stringResource(R.string.to_next_tier, thousands(l.pointsToNextTier)), color = c.mu, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Stats(l: Loyalty) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stat(Modifier.weight(1f), l.ticketsWatched, R.string.ds_stat_tickets)
        Stat(Modifier.weight(1f), l.vouchers, R.string.ds_stat_vouchers)
        Stat(Modifier.weight(1f), l.reviews, R.string.ds_stat_reviews)
    }
}

@Composable
private fun Stat(modifier: Modifier, value: Long, label: Int) {
    val c = Ceta.colors
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(c.sf).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = c.tx, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(label), color = c.mu, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Menu(icon: ImageVector, label: Int, onClick: () -> Unit) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = c.mu)
        Spacer(Modifier.width(14.dp))
        Text(stringResource(label), color = c.tx, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, tint = c.mu)
    }
}

/** 2450 -> "2.450", the way the design writes points. */
private fun thousands(n: Long): String = "%,d".format(n).replace(',', '.')
