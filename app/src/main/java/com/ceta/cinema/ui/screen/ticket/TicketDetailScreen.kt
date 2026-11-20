package com.ceta.cinema.ui.screen.ticket

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.style.TextDecoration
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
import com.ceta.cinema.ui.theme.cardSurface

@Composable
fun TicketDetailScreen(onBack: () -> Unit, onCancelled: () -> Unit = {}, vm: TicketDetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    BrightestWhileShown()
    val context = LocalContext.current
    val soon = stringResource(R.string.ds_soon)
    val content = (state as? UiState.Content)?.data
    LaunchedEffect(Unit) { vm.events.collect { if (it == TicketDetailEvent.Cancelled) onCancelled() } }
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircleButton(Icons.Rounded.ArrowBack, stringResource(R.string.back), onBack)
            Text(stringResource(R.string.ds_eticket), color = c.tx, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            if (content != null) CircleButton(Icons.Rounded.Share, stringResource(R.string.share)) { share(context, content) }
        }
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.error, vm::load)
            is UiState.Empty -> Unit
            is UiState.Content -> Content(s.data, soon, vm)
        }
    }
}

@Composable
private fun CircleButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val c = Ceta.colors
    Box(Modifier.size(44.dp).clip(CircleShape).background(c.sf).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = c.tx, modifier = Modifier.size(21.dp))
    }
}

private fun share(context: Context, ui: TicketDetailUi) {
    val t = ui.tickets.first()
    val text = "${t.movieTitle} · ${t.cinemaName.orEmpty()} · ${Formatters.date(Formatters.localDate(t.start))} ${Formatters.time(t.start)} · ${ui.tickets.joinToString(", ") { it.seatLabel }}"
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
}

private fun directions(context: Context, ticket: Ticket) {
    val where = listOfNotNull(ticket.cinemaName, ticket.cinemaAddress).joinToString(", ")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(where)))) }
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
private fun Content(ui: TicketDetailUi, soon: String, vm: TicketDetailViewModel) {
    val c = Ceta.colors
    val context = LocalContext.current
    val lead = ui.tickets.first()
    val seatsText = ui.tickets.joinToString(", ") { it.seatLabel }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirming by rememberSaveable { mutableStateOf(false) }
    val errorText = ui.cancelError?.let { stringResource(com.ceta.cinema.core.ErrorMapper.messageFor(it)) }
    LaunchedEffect(ui.cancelError) { if (errorText != null) { snackbar.showSnackbar(errorText); vm.consumeCancelError() } }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)) {
            Column(Modifier.fillMaxWidth().cardSurface(RoundedCornerShape(28.dp))) {
                Header(lead)
                Fields(lead, ui, seatsText)
                Perforation()
                QrBlock(ui.tickets, ui.cancelled)
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton(Modifier.weight(1f), Icons.Rounded.Wallet, R.string.ds_save_wallet) { scope.launch { snackbar.showSnackbar(soon) } }
                ActionButton(Modifier.weight(1f), Icons.Rounded.Directions, R.string.ds_directions) { directions(context, lead) }
            }
            if (ui.canCancel) Text(
                stringResource(R.string.cancel_ticket), color = c.mu, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().height(48.dp).clickable { confirming = true }.wrapContentHeight(Alignment.CenterVertically),
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }
    if (confirming) CancelSheet(ui, seatsText, onKeep = { confirming = false }, onConfirm = { confirming = false; vm.cancel() })
}

/** "Cancel this ticket?": what comes back, then Keep / Cancel (design item 9). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CancelSheet(ui: TicketDetailUi, seatsText: String, onKeep: () -> Unit, onConfirm: () -> Unit) {
    val c = Ceta.colors
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onKeep, containerColor = c.sf, scrimColor = Color.Black.copy(alpha = 0.58f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 4.dp).size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(c.ln2)) },
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.cancel_sheet_title), color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.cancel_sheet_body), color = c.mu, fontSize = 14.sp, lineHeight = 21.sp)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.sf2).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(ui.tickets.first().movieTitle, color = c.tx, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(seatsText, color = c.mu, fontSize = 14.sp)
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel_points_back), color = c.mu, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(MoneyFormatter.format(ui.refund), color = c.ac, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedButton(
                    onKeep, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(26.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, c.ln2),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = c.tx),
                ) { Text(stringResource(R.string.cancel_keep), fontWeight = FontWeight.Bold) }
                androidx.compose.material3.Button(
                    onConfirm, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(26.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = c.ac, contentColor = Color.White),
                ) { Text(stringResource(R.string.cancel_confirm), fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/** Poster, format chip and title over a darkening gradient. */
@Composable
private fun Header(t: Ticket) {
    Box(Modifier.fillMaxWidth().height(150.dp)) {
        Poster(t.posterUrl, t.movieTitle, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.25f), 0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
        // The stub's masthead: the logo in its inverted form (white tile, accent C), the name, and "ADMIT ONE" on the right.
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            CetaWordmark(26.dp, 15.sp, tile = Color.White, mark = Ceta.colors.ac, textColor = Color.White, spacingEm = 0.02f, gap = 7.dp)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.ticket_admit_one), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, letterSpacing = 1.1.sp)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                AgeBadge(t.ageRating)
                Text(
                    t.format.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(Color.White.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Text(t.movieTitle, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 6.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Fields(t: Ticket, ui: TicketDetailUi, seatsText: String) {
    val time = Formatters.time(t.start) + (t.end?.let { " – " + Formatters.time(it) } ?: "")
    val combo = ui.combos.joinToString(", ") { "${it.name} × ${it.quantity}" }.ifEmpty { stringResource(R.string.ds_none) }
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(Modifier.weight(1f), R.string.ds_cinema, t.cinemaName ?: "—"); Field(Modifier.weight(1f), R.string.ds_room, t.roomName)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(Modifier.weight(1f), R.string.ds_date, Formatters.date(Formatters.localDate(t.start))); Field(Modifier.weight(1f), R.string.ds_time, time)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(Modifier.weight(1f), R.string.ds_seats, seatsText, size = 22); Field(Modifier.weight(1f), R.string.ds_combo_t, combo, size = 13)
        }
    }
}

@Composable
private fun Field(modifier: Modifier, label: Int, value: String, size: Int = 15) {
    val c = Ceta.colors
    Column(modifier) {
        Text(stringResource(label), color = c.mu, fontSize = 12.sp)
        Text(value, color = c.tx, fontSize = size.sp, fontWeight = FontWeight.Bold)
    }
}

/** The tear-off line (design item 7): a 32dp notch in the page colour at each edge, half of it outside the card, and a row of round dots. */
@Composable
private fun Perforation() {
    val c = Ceta.colors
    Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(28.dp)) {
            val r = 16.dp.toPx()
            drawCircle(c.bg, r, Offset(0f, size.height / 2f))
            drawCircle(c.bg, r, Offset(size.width, size.height / 2f))
            // 4dp dots every 12dp, centred between the 22dp margins.
            val pitch = 12.dp.toPx()
            val from = 22.dp.toPx()
            val room = size.width - 2 * from
            val count = (room / pitch).toInt().coerceAtLeast(1)
            val start = from + (room - (count - 1) * pitch) / 2f
            for (i in 0 until count) drawCircle(c.ln2, 2.dp.toPx(), Offset(start + i * pitch, size.height / 2f))
        }
    }
}

@Composable
private fun QrBlock(tickets: List<Ticket>, cancelled: Boolean) {
    val c = Ceta.colors
    val pager = rememberPagerState(pageCount = { tickets.size })
    Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalPager(pager, Modifier.fillMaxWidth()) { page -> QrCard(tickets[page], page, tickets.size, cancelled) }
        Text(stringResource(R.string.ds_show_qr), color = c.mu, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp, modifier = Modifier.widthIn(max = 260.dp))
    }
}

@Composable
private fun ActionButton(modifier: Modifier, icon: ImageVector, label: Int, onClick: () -> Unit) {
    val c = Ceta.colors
    Row(
        modifier.height(52.dp).cardSurface(RoundedCornerShape(26.dp)).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.tx, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(label), color = c.tx, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** One seat's QR on white (scanners need dark-on-light whatever the theme), with its text code under it. */
@Composable
private fun QrCard(ticket: Ticket, index: Int, count: Int, cancelled: Boolean) {
    val matrix = remember(ticket.qrPayload) { QrMatrix.of(ticket.qrPayload) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).padding(12.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(175.dp).alpha(if (ticket.used || cancelled) 0.15f else 1f)) {
                val n = matrix.size
                val cell = size.width / n
                for (y in 0 until n) for (x in 0 until n) if (matrix[y][x]) drawRect(Color(0xFF141416), Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
            }
            if (ticket.used || cancelled) {
                Text(
                    stringResource(if (cancelled) R.string.stamp_cancelled else R.string.ds_used), color = Color(0xFF666666), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 1.6.sp,
                    modifier = Modifier.rotate(-12f).border(3.dp, Color(0xFF888888), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Text(ticket.code, color = Ceta.colors.tx, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.16.sp, fontFamily = FontFamily.Monospace)
        if (count > 1) Text("${ticket.seatLabel}  (${index + 1}/$count)", color = Ceta.colors.mu, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
