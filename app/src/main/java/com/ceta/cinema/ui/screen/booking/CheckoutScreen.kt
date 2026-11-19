package com.ceta.cinema.ui.screen.booking

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
import com.ceta.cinema.domain.model.PaymentProvider
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.theme.Ceta

@Composable
fun CheckoutScreen(onBack: () -> Unit, onPaying: (Long) -> Unit, onVietQr: (Long, String, String) -> Unit, onChooseSeats: () -> Unit, onHoldExpired: () -> Unit = {}, vm: CheckoutViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    val context = LocalContext.current
    EverySecond(vm::tick)
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            if (event is CheckoutEvent.ShowVietQr) onVietQr(event.bookingId, event.imageUrl, event.note)
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
            is UiState.Content -> Content(s.data, vm, onBack, onChooseSeats, onHoldExpired)
        }
    }
}

@Composable
private fun Content(ui: CheckoutUi, vm: CheckoutViewModel, onBack: () -> Unit, onChooseSeats: () -> Unit, onHoldExpired: () -> Unit) {
    val c = Ceta.colors
    val b = ui.booking
    val snackbar = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }
    HoldToasts("b${b.id}", ui.warning, ui.expired, snackbar, onHoldExpired)
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.sf).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, stringResource(R.string.back), tint = c.tx, modifier = Modifier.size(22.dp))
            }
            Text(stringResource(R.string.ds_payment), color = c.tx, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            HoldChip(ui.secondsLeft, ui.warning)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Summary(ui)
            if (ui.expired) {
                Text(stringResource(R.string.ds_hold_expired), color = c.ac, style = MaterialTheme.typography.bodyMedium)
                PrimaryButton(stringResource(R.string.choose_seats_again), onChooseSeats)
            } else {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    VoucherBox(ui, vm)
                    if (ui.loyalty != null) { HorizontalDivider(color = c.ln); PointsRow(ui, vm) }
                }
            }
            MethodRow(ui, vm::selectMethod)
            Lines(ui)
            ui.error?.let { Text(stringResource(ErrorMapper.messageFor(it)), color = c.ac, style = MaterialTheme.typography.bodySmall) }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(c.sf).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp)) {
            OrderSummaryLine(b.seats.map { it.label }, b.combos.sumOf { it.quantity }, Modifier.padding(bottom = 8.dp))
            if (ui.warning && !ui.expired) Text(stringResource(R.string.hold_pay_now), color = c.mu, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 6.dp))
            PrimaryButton(stringResource(when (ui.method) { PaymentProvider.MOMO -> R.string.pay_with_momo; PaymentProvider.VIETQR -> R.string.pay_with_vietqr; else -> R.string.pay_with_vnpay }, MoneyFormatter.format(b.totalAmount)), vm::pay, enabled = ui.canPay, loading = ui.working)
        }
    }
    androidx.compose.material3.SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
    }
}

@Composable
private fun Summary(ui: CheckoutUi) {
    val c = Ceta.colors
    val b = ui.booking
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf).padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(b.movieTitle.orEmpty(), color = c.tx, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        b.roomName?.let { Text(it, color = c.mu, fontSize = 13.sp) }
        b.startTime?.let { Text(Formatters.date(Formatters.localDate(it)) + " · " + Formatters.time(it), color = c.tx, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(b.seats.joinToString(", ") { it.label }, color = c.ac, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(c.acs).padding(horizontal = 8.dp, vertical = 3.dp))
        }
        Text(stringResource(R.string.checkout_order, b.id), color = c.mu, fontSize = 11.sp)
    }
}

@Composable
private fun VoucherBox(ui: CheckoutUi, vm: CheckoutViewModel) {
    val c = Ceta.colors
    val b = ui.booking
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.ds_voucher), color = c.tx, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ui.voucherInput, onValueChange = vm::onVoucher, singleLine = true, modifier = Modifier.weight(1f).height(52.dp),
                placeholder = { Text(stringResource(R.string.ds_voucher_ph)) }, isError = ui.voucherError != null,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 15.sp, color = c.tx),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = c.bg, unfocusedContainerColor = c.bg, focusedBorderColor = c.ac, unfocusedBorderColor = c.ln2),
            )
            val enabled = ui.voucherInput.isNotBlank() && ui.voucherInput != b.voucherCode && !ui.working
            Box(
                Modifier.height(46.dp).alpha(if (enabled) 1f else 0.4f).clip(RoundedCornerShape(14.dp)).background(c.tx).clickable(enabled = enabled, onClick = vm::applyVoucher).padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.ds_apply), color = c.bg, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
        ui.voucherError?.let { Text(stringResource(ErrorMapper.messageFor(it)), color = c.ac, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        if (b.voucherCode != null) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.voucher_applied, b.voucherCode, MoneyFormatter.format(b.discountAmount)), color = Color(0xFF3FA66B), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(vm::removeVoucher, enabled = !ui.working) { Text(stringResource(R.string.voucher_remove), color = c.mu) }
        }
    }
}

@Composable
private fun PointsRow(ui: CheckoutUi, vm: CheckoutViewModel) {
    val l = ui.loyalty ?: return
    val c = Ceta.colors
    val enabled = (ui.canUsePoints || ui.usingPoints) && !ui.working
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Rounded.Stars, null, tint = c.ac2, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.points_use, "%,d".format(l.redeemBlockPoints).replace(',', '.')), color = c.tx, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (ui.canUsePoints || ui.usingPoints) stringResource(R.string.points_sub, MoneyFormatter.format(l.redeemBlockVnd), "%,d".format(l.balance).replace(',', '.'))
                else stringResource(R.string.points_need, "%,d".format(l.redeemBlockPoints).replace(',', '.'), "%,d".format(l.balance).replace(',', '.')),
                color = c.mu, fontSize = 12.sp,
            )
        }
        Switch(checked = ui.usingPoints, onCheckedChange = { vm.togglePoints() }, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = c.ac, checkedThumbColor = Color.White, uncheckedTrackColor = c.ln2, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent))
    }
}

/** Every method the server offers, as the design's selectable rows; the chosen one has the accent border and a filled radio. */
@Composable
private fun MethodRow(ui: CheckoutUi, onSelect: (PaymentProvider) -> Unit) {
    val c = Ceta.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.ds_method), color = c.tx, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
        ui.methods.forEach { method ->
            val on = method == ui.method
            val (badge, color, label) = when (method) {
                PaymentProvider.VNPAY -> Triple("VN", Color(0xFFD64B3A), R.string.vnpay_name)
                PaymentProvider.MOMO -> Triple("Mo", Color(0xFFA50064), R.string.momo_name)
                PaymentProvider.VIETQR -> Triple("QR", Color(0xFF0E7C86), R.string.vietqr_name)
            }
            Row(
                Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(18.dp)).background(c.sf)
                    .border(1.5.dp, if (on) c.ac else Color.Transparent, RoundedCornerShape(18.dp)).clickable { onSelect(method) }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color), contentAlignment = Alignment.Center) {
                    Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(stringResource(label), color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(Modifier.size(22.dp).border(2.dp, if (on) c.ac else c.ln2, CircleShape), contentAlignment = Alignment.Center) {
                    if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(c.ac))
                }
            }
        }
    }
}

@Composable
private fun Lines(ui: CheckoutUi) {
    val b = ui.booking
    val c = Ceta.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Line(stringResource(R.string.ds_tickets) + " × " + b.seats.size, MoneyFormatter.format(b.seatsAmount))
        if (b.comboAmount > 0) Line(stringResource(R.string.ds_combo_line), MoneyFormatter.format(b.comboAmount))
        if (b.discountAmount > 0) Line(stringResource(R.string.ds_discount), MoneyFormatter.format(-b.discountAmount), accent = true)
        if (b.pointsAmount > 0) Line(stringResource(R.string.ds_points_line), MoneyFormatter.format(-b.pointsAmount), accent = true)
        HorizontalDivider(color = c.ln, modifier = Modifier.padding(vertical = 4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.ds_total), color = c.tx, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(MoneyFormatter.format(b.totalAmount), color = c.tx, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
    }
}

@Composable
private fun Line(label: String, value: String, accent: Boolean = false) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = c.mu, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = if (accent) c.ac else c.tx, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
