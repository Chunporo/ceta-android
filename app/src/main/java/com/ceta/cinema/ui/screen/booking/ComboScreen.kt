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
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
fun ComboScreen(onBack: () -> Unit, onNext: () -> Unit, onChooseSeats: () -> Unit, vm: ComboViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    EverySecond(vm::tick)
    LaunchedEffect(Unit) { vm.next.collect { onNext() } }
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        when (val s = state) {
            UiState.Loading -> { CetaTopBar(stringResource(R.string.ds_combo), onBack); LoadingBox() }
            is UiState.Error -> { CetaTopBar(stringResource(R.string.ds_combo), onBack); ErrorBox(s.error, vm::load) }
            is UiState.Empty -> Unit
            is UiState.Content -> Content(s.data, vm, onBack, onChooseSeats)
        }
    }
}

@Composable
private fun Content(ui: ComboUi, vm: ComboViewModel, onBack: () -> Unit, onChooseSeats: () -> Unit) {
    val c = Ceta.colors
    val english = LocalConfiguration.current.locales[0].language == "en"
    Column(Modifier.fillMaxSize()) {
        CetaTopBar(stringResource(R.string.ds_combo), onBack)
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ds_combo_sub), color = c.mu, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
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
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (ui.combos.isEmpty()) item { Text(stringResource(R.string.combo_none_available), color = c.mu, style = MaterialTheme.typography.bodyMedium) }
            items(ui.combos, key = { it.id }) { combo -> ComboRow(combo, english, ui.quantities[combo.id] ?: 0) { vm.change(combo.id, it) } }
            ui.error?.let { item { Text(stringResource(ErrorMapper.messageFor(it)), color = c.ac, style = MaterialTheme.typography.bodySmall) } }
        }
        Column(Modifier.fillMaxWidth().background(c.sf).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.combo_total_label), color = c.mu, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(MoneyFormatter.format(ui.total), color = c.tx, style = MaterialTheme.typography.titleMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineButton(stringResource(R.string.ds_skip_combo), { vm.proceed(skip = true) }, Modifier.weight(1f), enabled = !ui.saving)
                Box(Modifier.weight(1f)) { PrimaryButton(stringResource(R.string.ds_cont), { vm.proceed(skip = false) }, loading = ui.saving) }
            }
        }
    }
}

@Composable
private fun ComboRow(combo: Combo, english: Boolean, quantity: Int, onChange: (Int) -> Unit) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.sf).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Poster(combo.imageUrl, "combo${combo.id}", Modifier.size(72.dp))
        Column(Modifier.weight(1f)) {
            Text(combo.title(english), color = c.tx, style = MaterialTheme.typography.titleSmall)
            combo.detail(english)?.let { Text(it, color = c.mu, style = MaterialTheme.typography.bodySmall, maxLines = 2) }
            Text(MoneyFormatter.format(combo.price), color = c.ac, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
        }
        Stepper(quantity, onChange)
    }
}

@Composable
private fun Stepper(quantity: Int, onChange: (Int) -> Unit) {
    val c = Ceta.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(Icons.Rounded.Remove, enabled = quantity > 0) { onChange(-1) }
        Text(quantity.toString(), color = c.tx, style = MaterialTheme.typography.titleSmall, modifier = Modifier.widthIn(min = 28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        StepButton(Icons.Rounded.Add, enabled = quantity < ComboViewModel.MAX_PER_COMBO) { onChange(+1) }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val c = Ceta.colors
    Box(Modifier.size(32.dp).clip(CircleShape).background(if (enabled) c.acs else c.sf2).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (enabled) c.ac else c.mu, modifier = Modifier.size(18.dp))
    }
}
