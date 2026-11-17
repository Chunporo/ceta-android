package com.ceta.cinema.ui.screen.offers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.ErrorMapper
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.Promotion
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.screen.home.OfflineBanner
import com.ceta.cinema.ui.theme.Ceta

@Composable
fun OffersTab(onLogin: () -> Unit, vm: OffersViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val offline by vm.offline.collectAsState()
    val c = Ceta.colors
    val snackbar = remember { SnackbarHostState() }
    val ui = (state as? UiState.Content)?.data
    val savedText = stringResource(R.string.ds_copied)
    val loginText = stringResource(R.string.login_needed)
    val problemText = ui?.problem?.let { stringResource(ErrorMapper.messageFor(it)) }
    LaunchedEffect(ui?.justSaved, ui?.problem, ui?.needsLogin) {
        when {
            ui?.needsLogin == true -> { snackbar.showSnackbar(loginText); vm.consumeMessage(); onLogin() }
            ui?.justSaved == true -> { snackbar.showSnackbar(savedText); vm.consumeMessage() }
            problemText != null -> { snackbar.showSnackbar(problemText); vm.consumeMessage() }
        }
    }
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Text(stringResource(R.string.ds_promos_title), style = MaterialTheme.typography.titleLarge, color = c.tx, modifier = Modifier.padding(20.dp))
            if (offline) OfflineBanner()
            when (val s = state) {
                UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.error, vm::load)
                is UiState.Empty -> Unit
                is UiState.Content -> if (s.data.offers.isEmpty()) EmptyState(stringResource(R.string.ds_no_results))
                else LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(s.data.offers, key = { it.id }) { OfferCard(it, it.id in s.data.saved, it.id in s.data.busy) { vm.toggleSave(it) } }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }
}

@Composable
private fun OfferCard(promo: Promotion, saved: Boolean, busy: Boolean, onToggle: () -> Unit) {
    val c = Ceta.colors
    val english = LocalConfiguration.current.locales[0].language == "en"
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.sf).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(promo.title(english), color = c.tx, style = MaterialTheme.typography.titleMedium)
        promo.detail(english)?.let { Text(it, color = c.mu, style = MaterialTheme.typography.bodyMedium) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(promo.code, color = c.ac, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(c.acs).padding(horizontal = 10.dp, vertical = 4.dp))
            promo.validUntil?.let { Text(stringResource(R.string.ds_until) + Formatters.date(Formatters.localDate(it)), color = c.mu, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.weight(1f))
            val fg = if (saved) c.tx else Color.White
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).background(if (saved) c.sf2 else c.ac).clickable(enabled = !busy, onClick = onToggle).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(if (saved) Icons.Rounded.Check else Icons.Rounded.Add, null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(if (saved) R.string.ds_saved else R.string.ds_save_code), color = fg, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
