package com.ceta.cinema.ui.screen.booking

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCardOff
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.ErrorMapper
import com.ceta.cinema.core.MoneyFormatter
import com.ceta.cinema.domain.model.PaymentProvider
import com.ceta.cinema.ui.component.EverySecond
import com.ceta.cinema.ui.component.HoldChip
import com.ceta.cinema.ui.component.PrimaryButton
import com.ceta.cinema.ui.component.OutlineButton
import com.ceta.cinema.ui.component.RingIcon
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface

@Composable
fun PayFailScreen(
    onClose: () -> Unit, onPickOther: () -> Unit, onSupport: () -> Unit, onCancelled: () -> Unit, onRetried: (Long) -> Unit,
    onHoldExpired: (Long?, List<String>) -> Unit, vm: PayFailViewModel = hiltViewModel(),
) {
    val ui by vm.state.collectAsState()
    val c = Ceta.colors
    val context = LocalContext.current
    EverySecond(vm::tick)
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is PayFailEvent.Retry -> {
                    runCatching { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(event.url)) }
                    onRetried(ui.bookingId)
                }
                PayFailEvent.Cancelled -> onCancelled()
            }
        }
    }
    LaunchedEffect(ui.expired) { if (ui.expired) onHoldExpired(ui.booking?.showtimeId, ui.booking?.seats?.map { it.label }.orEmpty()) }
    val method = stringResource(methodName(ui.method))
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.sf).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = c.tx, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            if (ui.booking != null) HoldChip(ui.secondsLeft, ui.warning)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(12.dp))
            RingIcon(Icons.Rounded.CreditCardOff)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.payfail_title), color = c.tx, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.payfail_sub, method), color = c.mu, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Summary(ui, method)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.payfail_support), color = c.mu, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onSupport).padding(vertical = 8.dp),
            )
            ui.error?.let { Text(stringResource(ErrorMapper.messageFor(it)), color = c.ac, fontSize = 13.sp, textAlign = TextAlign.Center) }
            Spacer(Modifier.height(12.dp))
        }
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(stringResource(R.string.payfail_retry, method), vm::retry, enabled = !ui.expired, loading = ui.working, pill = true)
            OutlineButton(stringResource(R.string.payfail_other), onPickOther, pill = true)
            Text(
                stringResource(R.string.payfail_cancel), color = c.ac, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().height(44.dp).clickable(enabled = !ui.working, onClick = vm::cancelBooking).wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
private fun Summary(ui: PayFailUi, method: String) {
    val c = Ceta.colors
    val b = ui.booking
    Column(Modifier.fillMaxWidth().cardSurface(RoundedCornerShape(24.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.acs), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.EventSeat, null, tint = c.ac, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(stringResource(R.string.payfail_held), color = c.tx, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(listOfNotNull(b?.seats?.joinToString(", ") { it.label }, ui.show?.format?.label).joinToString(" · "), color = c.mu, fontSize = 13.sp)
            }
        }
        HorizontalDivider(color = c.ln)
        Row(label = stringResource(R.string.payfail_movie), value = b?.movieTitle.orEmpty())
        Row(label = stringResource(R.string.payfail_amount), value = b?.let { MoneyFormatter.format(it.totalAmount) }.orEmpty())
        Row(label = stringResource(R.string.payfail_method), value = method)
    }
}

@Composable
private fun Row(label: String, value: String) {
    val c = Ceta.colors
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = c.mu, fontSize = 14.sp)
        Text(value, color = c.tx, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

private fun methodName(method: PaymentProvider): Int = when (method) {
    PaymentProvider.MOMO -> R.string.method_short_momo
    PaymentProvider.VIETQR -> R.string.method_short_vietqr
    else -> R.string.method_short_vnpay
}
