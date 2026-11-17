package com.ceta.cinema.ui.screen.booking

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.ErrorMapper
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.MoneyFormatter
import com.ceta.cinema.core.UiState
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.theme.Ceta

@Composable
fun CheckoutScreen(onBack: () -> Unit, onPaying: (Long) -> Unit, onChooseSeats: () -> Unit, vm: CheckoutViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    val context = LocalContext.current
    EverySecond(vm::tick)
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            if (event is CheckoutEvent.OpenPayment) {
                // VNPay opens in a Custom Tab (not a WebView: saved cards work and banks accept it); the result is asked of the server.
                runCatching { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(event.url)) }
                onPaying(event.bookingId)
            }
        }
    }
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        when (val s = state) {
            UiState.Loading -> { CetaTopBar(stringResource(R.string.ds_payment), onBack); LoadingBox() }
            is UiState.Error -> { CetaTopBar(stringResource(R.string.ds_payment), onBack); ErrorBox(s.error, vm::load) }
            is UiState.Empty -> Unit
            is UiState.Content -> Content(s.data, vm, onBack, onChooseSeats)
        }
    }
}

@Composable
private fun Content(ui: CheckoutUi, vm: CheckoutViewModel, onBack: () -> Unit, onChooseSeats: () -> Unit) {
    val c = Ceta.colors
    val b = ui.booking
    Column(Modifier.fillMaxSize()) {
        CetaTopBar(stringResource(R.string.ds_payment), onBack) { HoldChip(ui.secondsLeft, ui.warning, Modifier.padding(end = 12.dp)) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Summary(ui)
            if (ui.expired) {
                Text(stringResource(R.string.ds_hold_expired), color = c.ac, style = MaterialTheme.typography.bodyMedium)
                PrimaryButton(stringResource(R.string.choose_seats_again), onChooseSeats)
            } else {
                VoucherBox(ui, vm)
                PointsRow(ui, vm)
            }
            MethodRow()
            Lines(ui)
            ui.error?.let { Text(stringResource(ErrorMapper.messageFor(it)), color = c.ac, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.fillMaxWidth().background(c.sf).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
            if (ui.warning && !ui.expired) Text(stringResource(R.string.hold_pay_now), color = c.mu, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 6.dp))
            PrimaryButton(stringResource(R.string.pay_with_vnpay, MoneyFormatter.format(b.totalAmount)), vm::pay, enabled = ui.canPay, loading = ui.working)
        }
    }
}

@Composable
private fun Summary(ui: CheckoutUi) {
    val c = Ceta.colors
    val b = ui.booking
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.sf).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(b.movieTitle.orEmpty(), color = c.tx, style = MaterialTheme.typography.titleMedium)
        Text(listOfNotNull(b.roomName, b.startTime?.let { Formatters.time(it) + " · " + Formatters.date(Formatters.localDate(it)) }).joinToString(" · "), color = c.mu, style = MaterialTheme.typography.bodySmall)
        Text(b.seats.joinToString(", ") { it.label }, color = c.tx, style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.checkout_order, b.id), color = c.mu, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun VoucherBox(ui: CheckoutUi, vm: CheckoutViewModel) {
    val c = Ceta.colors
    val b = ui.booking
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.ds_voucher), color = c.mu, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ui.voucherInput, onValueChange = vm::onVoucher, singleLine = true, modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.ds_voucher_ph)) }, isError = ui.voucherError != null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                shape = RoundedCornerShape(14.dp),
            )
            Box(Modifier.width(110.dp)) { PrimaryButton(stringResource(R.string.ds_apply), vm::applyVoucher, enabled = ui.voucherInput.isNotBlank() && ui.voucherInput != b.voucherCode && !ui.working) }
        }
        ui.voucherError?.let { Text(stringResource(ErrorMapper.messageFor(it)), color = c.ac, style = MaterialTheme.typography.bodySmall) }
        if (b.voucherCode != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.voucher_applied, b.voucherCode, MoneyFormatter.format(b.discountAmount)), color = Color(0xFF3FA66B), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(vm::removeVoucher, enabled = !ui.working) { Text(stringResource(R.string.voucher_remove), color = c.mu) }
        }
    }
}

@Composable
private fun PointsRow(ui: CheckoutUi, vm: CheckoutViewModel) {
    val l = ui.loyalty ?: return
    val c = Ceta.colors
    val enabled = (ui.canUsePoints || ui.usingPoints) && !ui.working
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.sf).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.points_use, "%,d".format(l.redeemBlockPoints).replace(',', '.')), color = c.tx, style = MaterialTheme.typography.titleSmall)
            Text(
                if (ui.canUsePoints || ui.usingPoints) stringResource(R.string.points_sub, MoneyFormatter.format(l.redeemBlockVnd), "%,d".format(l.balance).replace(',', '.'))
                else stringResource(R.string.points_need, "%,d".format(l.redeemBlockPoints).replace(',', '.'), "%,d".format(l.balance).replace(',', '.')),
                color = c.mu, style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = ui.usingPoints, onCheckedChange = { vm.togglePoints() }, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = c.ac, checkedThumbColor = Color.White))
    }
}

@Composable
private fun MethodRow() {
    val c = Ceta.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.ds_method), color = c.mu, style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.5.dp, c.ac, RoundedCornerShape(16.dp)).background(c.acs).padding(14.dp)) {
            Text(stringResource(R.string.vnpay_name), color = c.tx, style = MaterialTheme.typography.titleSmall)
        }
        Text(stringResource(R.string.other_methods_soon), color = c.mu, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Lines(ui: CheckoutUi) {
    val b = ui.booking
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Line(stringResource(R.string.ds_tickets), MoneyFormatter.format(b.seatsAmount))
        if (b.comboAmount > 0) Line(stringResource(R.string.ds_combo_line), MoneyFormatter.format(b.comboAmount))
        if (b.discountAmount > 0) Line(stringResource(R.string.ds_discount), MoneyFormatter.format(-b.discountAmount), accent = true)
        if (b.pointsAmount > 0) Line(stringResource(R.string.ds_points_line), MoneyFormatter.format(-b.pointsAmount), accent = true)
        HorizontalDivider(color = Ceta.colors.ln2)
        Line(stringResource(R.string.ds_total), MoneyFormatter.format(b.totalAmount), bold = true)
    }
}

@Composable
private fun Line(label: String, value: String, bold: Boolean = false, accent: Boolean = false) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = if (bold) c.tx else c.mu, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, color = if (accent) Color(0xFF3FA66B) else c.tx, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
    }
}
