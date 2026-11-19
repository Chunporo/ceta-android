package com.ceta.cinema.ui.screen.seat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ceta.cinema.R
import com.ceta.cinema.core.ErrorMapper
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.MoneyFormatter
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.SeatKind
import com.ceta.cinema.domain.usecase.priceTags
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.theme.Ceta
import kotlinx.coroutines.delay

@Composable
fun SeatMapScreen(onBack: () -> Unit, onBooked: (Long) -> Unit, vm: SeatMapViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // While the screen is visible the map is refreshed every few seconds, so a seat someone else took shows up quickly.
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { delay(REFRESH_MS); vm.refresh() } }
    }
    LaunchedEffect(Unit) { vm.booked.collect(onBooked) }
    EverySecond(vm::tick)
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        when (val s = state) {
            UiState.Loading -> { CetaTopBar("", onBack); LoadingBox() }
            is UiState.Error -> { CetaTopBar("", onBack); ErrorBox(s.error, vm::load) }
            is UiState.Empty -> Unit
            is UiState.Content -> Content(s.data, vm, onBack)
        }
    }
}

private const val REFRESH_MS = 5_000L

@Composable
private fun Content(ui: SeatMapUi, vm: SeatMapViewModel, onBack: () -> Unit) {
    val c = Ceta.colors
    val snackbar = remember { SnackbarHostState() }
    val text = ui.message?.let { messageText(it) }
    LaunchedEffect(ui.message) { if (text != null) { snackbar.showSnackbar(text); vm.consumeMessage() } }
    val show = ui.showtime
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundIconButton(Icons.Rounded.ArrowBack, stringResource(R.string.back), onBack)
                Column(Modifier.weight(1f)) {
                    Text(show?.movieTitle.orEmpty(), color = c.tx, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (show != null) Text(
                        listOfNotNull(show.cinemaName, Formatters.date(Formatters.localDate(show.start)), Formatters.time(show.start), show.format.label).joinToString(" · "),
                        color = c.mu, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                ui.holdSecondsLeft?.let { HoldChip(it, ui.holdWarning) }
                val zoomed = zoom > 1.01f
                RoundIconButton(if (zoomed) Icons.Rounded.ZoomOut else Icons.Rounded.ZoomIn, stringResource(R.string.seatmap_hint), { zoom = if (zoomed) 1f else BUTTON_ZOOM })
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CurvedScreen()
                Spacer(Modifier.height(8.dp))
                SeatMapCanvas(ui.seats, ui.selected, ui.conflicts, ui.loneSeats.map { it.id }.toSet(), vm::onSeat, zoom, { zoom = it }, Modifier.padding(horizontal = 12.dp))
                Spacer(Modifier.height(20.dp))
                Legend()
                PriceTags(ui)
            }
            BottomBar(ui, vm::continueBooking)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 130.dp))
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val c = Ceta.colors
    Box(Modifier.size(44.dp).clip(CircleShape).background(c.sf).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = c.tx, modifier = Modifier.size(22.dp))
    }
}

/** The screen drawn as an arc with a soft glow under it, as in the design. */
@Composable
private fun CurvedScreen() {
    val c = Ceta.colors
    Box(Modifier.fillMaxWidth(0.88f).height(44.dp), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            val oval = Rect(0f, stroke / 2f, size.width, size.height * 2f)
            val glow = Path().apply { arcTo(oval, 180f, 180f, true); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
            clipRect { drawPath(glow, Brush.verticalGradient(listOf(c.acs, Color.Transparent), startY = 0f, endY = size.height * 0.8f)) }
            drawArc(c.ac, 180f, 180f, false, Offset(0f, stroke / 2f), Size(size.width, size.height * 2f), style = Stroke(stroke))
        }
        Text(stringResource(R.string.ds_screen), color = c.mu, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.3.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun Legend() {
    val c = Ceta.colors
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // fill, outline, dashed, label
        listOf(
            Triple(c.sf2, c.ln2, R.string.ds_legend_free), Triple(c.ac, c.ac, R.string.ds_legend_sel),
            Triple(Color.Transparent, c.ln2, R.string.ds_legend_sold),
            Triple(c.ac2.copy(alpha = 0.22f), c.ac2, R.string.ds_legend_vip), Triple(c.ac3.copy(alpha = 0.22f), c.ac3, R.string.ds_legend_couple),
        ).forEach { (fill, line, label) ->
            val sold = label == R.string.ds_legend_sold
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(fill).border(1.5.dp, line, RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                    if (sold) Text("×", color = c.mu, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
                Text(stringResource(label), color = c.mu, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PriceTags(ui: SeatMapUi) {
    val c = Ceta.colors
    val tags = ui.seats.priceTags()
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(SeatKind.STANDARD to R.string.ds_std, SeatKind.VIP to R.string.ds_legend_vip, SeatKind.COUPLE to R.string.ds_legend_couple).forEach { (kind, label) ->
            tags[kind]?.let {
                Text("${stringResource(label)} ${MoneyFormatter.format(it)}", color = c.tx, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(c.sf).padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun BottomBar(ui: SeatMapUi, onContinue: () -> Unit) {
    val c = Ceta.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(c.sf)
            .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (ui.loneSeats.isNotEmpty()) LoneSeatBanner(ui.loneSeats.joinToString(", ") { it.label })
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ds_seats_sel), color = c.mu, fontSize = 12.sp)
                Text(
                    if (ui.selected.isEmpty()) stringResource(R.string.ds_no_seat) else ui.selectedSeats.joinToString(", ") { it.label },
                    color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.ds_total), color = c.mu, fontSize = 12.sp)
                Text(MoneyFormatter.format(ui.total), color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        PrimaryButton(stringResource(R.string.continue_booking), onContinue, enabled = ui.selected.isNotEmpty(), loading = ui.submitting)
    }
}

/** A soft warning above Continue: it names the seat(s) that would be left alone and never stops the customer from going on. */
@Composable
private fun LoneSeatBanner(labels: String) {
    val c = Ceta.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.acs).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Warning, null, tint = c.ac, modifier = Modifier.size(20.dp))
        Text(stringResource(R.string.lone_seat_warning, labels), color = c.tx, fontSize = 13.sp, lineHeight = 18.85.sp)
    }
}

@Composable
private fun messageText(message: SeatMessage): String = when (message) {
    SeatMessage.MaxSeats -> stringResource(R.string.ds_max_seats)
    SeatMessage.CouplePartnerTaken -> stringResource(R.string.error_invalid_couple_seat)
    is SeatMessage.SeatsTaken -> stringResource(R.string.seat_taken, message.labels.joinToString(", "))
    is SeatMessage.Failed -> stringResource(ErrorMapper.messageFor(message.error))
    SeatMessage.HoldWarning -> stringResource(R.string.hold_warn_toast)
    SeatMessage.HoldExpired -> stringResource(R.string.ds_hold_expired)
}
