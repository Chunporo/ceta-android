package com.ceta.cinema.ui.screen.seat

import androidx.compose.foundation.background
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
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            CetaTopBar(show?.movieTitle.orEmpty(), onBack)
            if (show != null) Text(
                listOfNotNull(show.cinemaName, show.roomName, Formatters.time(show.start), show.format.label).joinToString(" · "),
                color = c.mu, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 20.dp),
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
                ScreenBar()
                Spacer(Modifier.height(12.dp))
                SeatMapCanvas(ui.seats, ui.selected, ui.conflicts, vm::onSeat, Modifier.padding(horizontal = 12.dp))
                Spacer(Modifier.height(12.dp))
                Legend()
                PriceTags(ui)
                Text(stringResource(R.string.seatmap_hint), color = c.mu, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            BottomBar(ui, vm::continueBooking)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp))
    }
}

@Composable
private fun ScreenBar() {
    val c = Ceta.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(c.ac.copy(alpha = 0.6f)))
        Text(stringResource(R.string.ds_screen), color = c.mu, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Legend() {
    val c = Ceta.colors
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            c.sf2 to R.string.ds_legend_free, c.ac to R.string.ds_legend_sel, c.mu.copy(alpha = 0.55f) to R.string.ds_legend_sold,
            c.ac2.copy(alpha = 0.5f) to R.string.ds_legend_vip, c.ac3.copy(alpha = 0.5f) to R.string.ds_legend_couple,
        ).forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(label), color = c.mu, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PriceTags(ui: SeatMapUi) {
    val c = Ceta.colors
    val tags = ui.seats.priceTags()
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(SeatKind.STANDARD to R.string.ds_std, SeatKind.VIP to R.string.ds_legend_vip, SeatKind.COUPLE to R.string.ds_legend_couple).forEach { (kind, label) ->
            tags[kind]?.let {
                Text("${stringResource(label)} ${MoneyFormatter.format(it)}", color = c.tx, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(c.sf).padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }
    }
}

@Composable
private fun BottomBar(ui: SeatMapUi, onContinue: () -> Unit) {
    val c = Ceta.colors
    Column(Modifier.fillMaxWidth().background(c.sf).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (ui.selected.isEmpty()) stringResource(R.string.ds_no_seat)
                    else stringResource(R.string.seats_count, ui.selected.size) + " · " + ui.selectedSeats.joinToString(", ") { it.label },
                    color = c.mu, style = MaterialTheme.typography.bodySmall,
                )
                Text(MoneyFormatter.format(ui.total), color = c.tx, style = MaterialTheme.typography.titleMedium)
            }
            Box(Modifier.width(160.dp)) { PrimaryButton(stringResource(R.string.continue_booking), onContinue, enabled = ui.selected.isNotEmpty(), loading = ui.submitting) }
        }
    }
}

@Composable
private fun messageText(message: SeatMessage): String = when (message) {
    SeatMessage.MaxSeats -> stringResource(R.string.ds_max_seats)
    SeatMessage.CouplePartnerTaken -> stringResource(R.string.error_invalid_couple_seat)
    is SeatMessage.SeatsTaken -> stringResource(R.string.seat_taken, message.labels.joinToString(", "))
    is SeatMessage.Failed -> stringResource(ErrorMapper.messageFor(message.error))
}
