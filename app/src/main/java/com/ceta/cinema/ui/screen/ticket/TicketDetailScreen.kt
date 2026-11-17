package com.ceta.cinema.ui.screen.ticket

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.MoneyFormatter
import com.ceta.cinema.core.QrMatrix
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.Ticket
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.findActivity
import com.ceta.cinema.ui.theme.Ceta

@Composable
fun TicketDetailScreen(onBack: () -> Unit, vm: TicketDetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    BrightestWhileShown()
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        CetaTopBar(stringResource(R.string.ds_eticket), onBack)
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.error, vm::load)
            is UiState.Empty -> Unit
            is UiState.Content -> Content(s.data)
        }
    }
}

/** The door scanner reads the QR faster on a bright screen: raise it while this screen is open, put it back on leaving. */
@Composable
private fun BrightestWhileShown() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        val before = window?.attributes?.screenBrightness
        window?.let { it.attributes = it.attributes.apply { screenBrightness = 1f } }
        onDispose { window?.let { it.attributes = it.attributes.apply { screenBrightness = before ?: android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } } }
    }
}

@Composable
private fun Content(ui: TicketDetailUi) {
    val c = Ceta.colors
    val pager = rememberPagerState(pageCount = { ui.tickets.size })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val lead = ui.tickets.first()
        Text(lead.movieTitle, style = MaterialTheme.typography.titleLarge, color = c.tx)
        Details(lead, ui)
        HorizontalPager(pager, Modifier.fillMaxWidth(), pageSpacing = 12.dp) { page -> QrCard(ui.tickets[page], page, ui.tickets.size) }
        Text(stringResource(R.string.ds_show_qr), color = c.mu, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Details(t: Ticket, ui: TicketDetailUi) {
    val c = Ceta.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.sf).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) { Field(Modifier.weight(1f), R.string.ds_cinema, t.cinemaName ?: "—"); Field(Modifier.weight(1f), R.string.ds_room, t.roomName) }
        Row(Modifier.fillMaxWidth()) { Field(Modifier.weight(1f), R.string.ds_date, Formatters.date(Formatters.localDate(t.start))); Field(Modifier.weight(1f), R.string.ds_time, Formatters.time(t.start)) }
        Row(Modifier.fillMaxWidth()) {
            Field(Modifier.weight(1f), R.string.ds_seats, ui.tickets.joinToString(", ") { it.seatLabel })
            Field(Modifier.weight(1f), R.string.ds_format, t.format.label)
        }
        Field(Modifier.fillMaxWidth(), R.string.ds_combo_t, ui.combos.joinToString(", ") { "${it.name} × ${it.quantity}" }.ifEmpty { stringResource(R.string.ds_none) })
    }
}

@Composable
private fun Field(modifier: Modifier, label: Int, value: String) {
    val c = Ceta.colors
    Column(modifier) {
        Text(stringResource(label), color = c.mu, style = MaterialTheme.typography.labelSmall)
        Text(value, color = c.tx, style = MaterialTheme.typography.bodyMedium)
    }
}

/** One seat's QR on white (scanners need dark-on-light whatever the theme), with its text code as a fallback. */
@Composable
private fun QrCard(ticket: Ticket, index: Int, count: Int) {
    val c = Ceta.colors
    val matrix = remember(ticket.qrPayload) { QrMatrix.of(ticket.qrPayload) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(260.dp).alpha(if (ticket.used) 0.15f else 1f)) {
                val n = matrix.size
                val cell = size.width / n
                for (y in 0 until n) for (x in 0 until n) if (matrix[y][x]) drawRect(Color(0xFF141416), Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
            if (ticket.used) Text(stringResource(R.string.ds_used), color = Color(0xFFCF3B33), style = MaterialTheme.typography.titleLarge)
        }
        Text(ticket.seatLabel + if (count > 1) "  (${index + 1}/$count)" else "", color = Color(0xFF141416), style = MaterialTheme.typography.titleMedium)
        Text(ticket.code, color = Color(0xFF55555C), style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
        Text(MoneyFormatter.format(ticket.price), color = Color(0xFF55555C), style = MaterialTheme.typography.labelSmall)
    }
}
