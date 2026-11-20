package com.ceta.cinema.ui.screen.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.ErrorMapper
import com.ceta.cinema.core.MoneyFormatter
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.Combo
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface

@Composable
fun ComboScreen(onBack: () -> Unit, onNext: () -> Unit, onChooseSeats: () -> Unit, onHoldExpired: (Long?, List<String>) -> Unit = { _, _ -> }, vm: ComboViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    EverySecond(vm::tick)
    LaunchedEffect(Unit) { vm.next.collect { onNext() } }
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        when (val s = state) {
            UiState.Loading -> { CetaTopBar(stringResource(R.string.ds_combo), onBack); LoadingBox() }
            is UiState.Error -> { CetaTopBar(stringResource(R.string.ds_combo), onBack); ErrorBox(s.error, vm::load) }
            is UiState.Empty -> Unit
            is UiState.Content -> Content(s.data, vm, onBack, onChooseSeats, onHoldExpired)
        }
    }
}

@Composable
private fun Content(ui: ComboUi, vm: ComboViewModel, onBack: () -> Unit, onChooseSeats: () -> Unit, onHoldExpired: (Long?, List<String>) -> Unit) {
    val c = Ceta.colors
    val english = LocalConfiguration.current.locales[0].language == "en"
    val snackbar = remember { SnackbarHostState() }
    HoldToasts("b${ui.booking.id}", ui.warning, ui.expired, snackbar) { onHoldExpired(ui.booking.showtimeId, ui.booking.seats.map { it.label }) }
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.sf).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, stringResource(R.string.back), tint = c.tx, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ds_combo), color = c.tx, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(stringResource(R.string.ds_combo_sub), color = c.mu, fontSize = 12.sp, maxLines = 1)
            }
            HoldChip(ui.secondsLeft, ui.warning)
        }
        if (ui.expired) {
            Column(Modifier.weight(1f).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.ds_hold_expired), color = c.tx, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(stringResource(R.string.choose_seats_again), onChooseSeats, Modifier.width(220.dp))
            }
            return
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (ui.combos.isEmpty()) item { Text(stringResource(R.string.combo_none_available), color = c.mu, style = MaterialTheme.typography.bodyMedium) }
            items(ui.combos, key = { it.id }) { combo -> ComboRow(combo, english, ui.quantities[combo.id] ?: 0) { vm.change(combo.id, it) } }
            ui.error?.let { item { Text(stringResource(ErrorMapper.messageFor(it)), color = c.ac, style = MaterialTheme.typography.bodySmall) } }
        }
        val any = ui.quantities.values.any { it > 0 }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(c.sf).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                OrderSummaryLine(ui.booking.seats.map { it.label }, ui.quantities.values.sum())
                Text(MoneyFormatter.format(ui.total), color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Box(Modifier.width(180.dp)) {
                PrimaryButton(stringResource(if (any) R.string.ds_cont else R.string.ds_skip_combo), { vm.proceed(skip = !any) }, loading = ui.saving)
            }
        }
    }
    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
}

@Composable
private fun ComboRow(combo: Combo, english: Boolean, quantity: Int, onChange: (Int) -> Unit) {
    val c = Ceta.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth().cardSurface(shape).border(1.5.dp, if (quantity > 0) c.ac else Color.Transparent, shape).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Poster(combo.imageUrl, "combo${combo.id}", Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(combo.title(english), color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            combo.detail(english)?.let { Text(it, color = c.mu, fontSize = 13.sp, lineHeight = 17.sp, maxLines = 3) }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(MoneyFormatter.format(combo.price), color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Stepper(quantity, onChange)
            }
        }
    }
}

@Composable
private fun Stepper(quantity: Int, onChange: (Int) -> Unit) {
    val c = Ceta.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StepButton(Icons.Rounded.Remove, filled = false, enabled = quantity > 0) { onChange(-1) }
        Text(quantity.toString(), color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
        StepButton(Icons.Rounded.Add, filled = true, enabled = quantity < ComboViewModel.MAX_PER_COMBO) { onChange(+1) }
    }
}

/** Minus is an outlined circle, plus a solid accent one, as in the design. */
@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = Ceta.colors
    Box(
        Modifier.size(34.dp).alpha(if (enabled) 1f else 0.35f).clip(CircleShape).background(if (filled) c.ac else Color.Transparent)
            .then(if (filled) Modifier else Modifier.border(1.5.dp, c.ln2, CircleShape)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (filled) Color.White else c.tx, modifier = Modifier.size(18.dp)) }
}
