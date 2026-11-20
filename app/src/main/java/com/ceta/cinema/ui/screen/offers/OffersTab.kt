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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.ErrorMapper
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.Promotion
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.screen.home.OfflineBanner
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface

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
            Text(stringResource(R.string.ds_promos_title), color = c.tx, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp))
            if (offline) OfflineBanner()
            when (val s = state) {
                UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.error, vm::load)
                is UiState.Empty -> Unit
                is UiState.Content -> if (s.data.offers.isEmpty()) EmptyState(stringResource(R.string.ds_no_results))
                else LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(s.data.offers, key = { it.id }) { OfferCard(it, it.id in s.data.saved, it.id in s.data.busy) { vm.toggleSave(it) } }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }
}

/** A banner in a colour taken from the offer's code, then its text, code and the save button, as in the design. */
@Composable
private fun OfferCard(promo: Promotion, saved: Boolean, busy: Boolean, onToggle: () -> Unit) {
    val c = Ceta.colors
    val english = LocalConfiguration.current.locales[0].language == "en"
    Column(Modifier.fillMaxWidth().cardSurface(RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))) {
        OfferBanner(promo, Modifier.fillMaxWidth().height(120.dp), highlightSize = 44.sp) {
            Text(
                promo.title(english), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp, maxLines = 3,
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(0.55f).padding(16.dp),
            )
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            promo.detail(english)?.let { Text(it, color = c.tx, fontSize = 14.sp, lineHeight = 21.sp) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(promo.code, color = c.tx, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    promo.validUntil?.let { Text(stringResource(R.string.ds_until) + " " + Formatters.date(Formatters.localDate(it)), color = c.mu, fontSize = 12.sp) }
                }
                val fg = if (saved) c.tx else Color.White
                Row(
                    Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(if (saved) c.sf2 else c.ac).clickable(enabled = !busy, onClick = onToggle).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Icon(if (saved) Icons.Rounded.Check else Icons.Rounded.Add, null, tint = fg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(if (saved) R.string.ds_saved else R.string.ds_save_code), color = fg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
