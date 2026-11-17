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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onHistory: () -> Unit = {}, onProfile: () -> Unit = {}, onVouchers: () -> Unit = {}, onReminders: () -> Unit = {},
    onPayments: () -> Unit = {}, onHelp: () -> Unit = {},
    vm: AccountViewModel = hiltViewModel(),
) {
    val c = Ceta.colors
    val loyalty by vm.loyalty.collectAsState()
    val name by vm.displayName.collectAsState()
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (signedIn) {
            Header(name, (loyalty as? UiState.Content)?.data, onProfile)
            when (val l = loyalty) {
                UiState.Loading -> Box(Modifier.fillMaxWidth().height(96.dp)) { CircularProgressIndicator(Modifier.align(Alignment.Center), color = c.ac) }
                is UiState.Error -> ErrorBox(l.error, vm::load, Modifier.height(140.dp))
                is UiState.Empty -> Unit
                is UiState.Content -> { PointsCard(l.data); Stats(l.data) }
            }
        } else {
            Text(stringResource(R.string.ds_nav_account), color = c.tx, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            EmptyState(stringResource(R.string.account_guest_title), cta = stringResource(R.string.ds_login), onCta = onSignIn)
        }
        // One card for settings and menu, as in the design: language, dark mode, then the account rows.
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Rounded.Translate, null, tint = c.mu, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.ds_lang), color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                LanguagePills(settings.language == "en") { onLanguage(if (it) "en" else "vi") }
            }
            RowDivider()
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).clickable { onDark(!settings.dark) }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Rounded.DarkMode, null, tint = c.mu, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.ds_dark_mode), color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Switch(checked = settings.dark, onCheckedChange = onDark, colors = SwitchDefaults.colors(checkedTrackColor = c.ac, checkedThumbColor = Color.White, uncheckedTrackColor = c.ln2, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent))
            }
            if (signedIn) {
                MenuRow(Icons.Rounded.Person, R.string.ds_profile, onProfile)
                MenuRow(Icons.Rounded.ConfirmationNumber, R.string.ds_my_vouchers, onVouchers)
                MenuRow(Icons.Rounded.NotificationsActive, R.string.reminders_title, onReminders)
                MenuRow(Icons.Rounded.ReceiptLong, R.string.ds_history, onHistory)
                MenuRow(Icons.Rounded.CreditCard, R.string.ds_pay_methods, onPayments)
                MenuRow(Icons.Rounded.HelpOutline, R.string.ds_help, onHelp)
                MenuRow(Icons.Rounded.Logout, R.string.ds_logout, onSignOut, danger = true)
            }
        }
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(color = Ceta.colors.ln, modifier = Modifier.padding(start = 52.dp))

@Composable
private fun LanguagePills(english: Boolean, onChange: (Boolean) -> Unit) {
    val c = Ceta.colors
    Row(Modifier.clip(RoundedCornerShape(16.dp)).background(c.sf2).padding(3.dp)) {
        listOf("VI" to false, "EN" to true).forEach { (label, value) ->
            val on = english == value
            Box(
                Modifier.height(30.dp).clip(RoundedCornerShape(13.dp)).background(if (on) c.ac else Color.Transparent).clickable { onChange(value) }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (on) Color.White else c.mu, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun Header(name: String?, loyalty: Loyalty?, onEdit: () -> Unit) {
    val c = Ceta.colors
    val initials = com.ceta.cinema.ui.screen.account.initialsOf(name)
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(60.dp).clip(CircleShape).background(c.ac2), contentAlignment = Alignment.Center) {
            Text(initials, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(Modifier.weight(1f)) {
            Text(name ?: "CETA", color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            loyalty?.let { Text(stringResource(if (it.isTopTier) R.string.tier_vip else R.string.tier_member), color = c.mu, fontSize = 13.sp) }
        }
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.sf).clickable(onClick = onEdit), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Edit, null, tint = c.tx, modifier = Modifier.size(21.dp))
        }
    }
}

/** The accent loyalty card with a soft circle in the corner, big balance and a white progress bar. */
@Composable
private fun PointsCard(l: Loyalty) {
    val c = Ceta.colors
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.ac)) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-40).dp).size(160.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)))
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("CETA STAR", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Column {
                Text(thousands(l.balance), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp)
                Text(stringResource(R.string.ds_points), color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Column {
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.25f))) {
                    Box(Modifier.fillMaxWidth(l.progressToNextTier.coerceAtLeast(0.02f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color.White))
                }
                Text(if (l.isTopTier) stringResource(R.string.top_tier) else stringResource(R.string.to_next_tier, thousands(l.pointsToNextTier)), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
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
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(c.sf).padding(14.dp)) {
        Text(value.toString(), color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(stringResource(label), color = c.mu, fontSize = 12.sp)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: Int, onClick: () -> Unit, danger: Boolean = false) {
    val c = Ceta.colors
    RowDivider()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, tint = if (danger) c.ac else c.mu, modifier = Modifier.size(22.dp))
        Text(stringResource(label), color = if (danger) c.ac else c.tx, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (!danger) Icon(Icons.Rounded.ChevronRight, null, tint = c.mu, modifier = Modifier.size(20.dp))
    }
}

/** 2450 -> "2.450", the way the design writes points. */
private fun thousands(n: Long): String = "%,d".format(n).replace(',', '.')
