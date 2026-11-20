package com.ceta.cinema.ui.screen.ticket

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.MoneyFormatter
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.usecase.CancelledTicket
import com.ceta.cinema.domain.usecase.ShowWhen
import com.ceta.cinema.domain.usecase.TicketGroup
import com.ceta.cinema.domain.usecase.TicketGrouping
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.screen.home.OfflineBanner
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface

/** The filled-star colour of the design (oklch 0.8 0.16 85). */
private val StarGold = Color(0xFFEDB42E)

/**
 * "My tickets" (design item 9): Upcoming / Watched / Cancelled. [startOnCancelled] opens the last tab, and [notice] is a one-time
 * message (the ticket was just cancelled) shown as a toast.
 */
@Composable
fun TicketsTab(
    onTicket: (Long) -> Unit, onBook: () -> Unit, onSignIn: () -> Unit,
    startOnCancelled: Boolean = false, notice: String? = null, onNoticeShown: () -> Unit = {},
    vm: TicketsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val signedIn by vm.signedIn.collectAsState()
    val offline by vm.offline.collectAsState()
    val ratings by vm.ratings.collectAsState()
    val c = Ceta.colors
    var segment by rememberSaveable { mutableIntStateOf(if (startOnCancelled) 2 else 0) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(notice) { if (notice != null) { snackbar.showSnackbar(notice); onNoticeShown() } }
    Box(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            Text(stringResource(R.string.ds_tickets_title), color = c.tx, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp))
            if (signedIn != true) {
                EmptyState(stringResource(R.string.login_needed), cta = stringResource(R.string.ds_login), onCta = onSignIn)
                return@Column
            }
            if (offline) OfflineBanner()
            SegmentedTabs(
                listOf(stringResource(R.string.ds_upcoming), stringResource(R.string.ds_watched), stringResource(R.string.tickets_cancelled)),
                segment, { segment = it }, Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(16.dp))
            when (val s = state) {
                UiState.Loading -> LoadingBox()
                is UiState.Error -> ErrorBox(s.error, onRetry = { vm.load(forceRefresh = true) })
                is UiState.Empty -> Unit
                is UiState.Content -> {
                    val data = s.data
                    when (segment) {
                        0 -> Tickets(data.upcoming, R.string.ds_empty_tickets, cta = true, onBook = onBook) { g ->
                            TicketCard(g, Mode.Upcoming, ratings, vm::rate) { onTicket(g.bookingId) }
                        }
                        1 -> Tickets(data.watched, R.string.tickets_empty_watched, cta = false, onBook = onBook) { g ->
                            TicketCard(g, Mode.Watched, ratings, vm::rate) { onTicket(g.bookingId) }
                        }
                        else -> Tickets(data.cancelled.map { it.group }, R.string.tickets_empty_cancelled, cta = false, onBook = onBook) { g ->
                            val item = data.cancelled.first { it.group.bookingId == g.bookingId }
                            TicketCard(g, Mode.Cancelled(item), ratings, vm::rate) { onTicket(g.bookingId) }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }
}

@Composable
private fun Tickets(list: List<TicketGroup>, empty: Int, cta: Boolean, onBook: () -> Unit, card: @Composable (TicketGroup) -> Unit) {
    if (list.isEmpty()) {
        EmptyState(stringResource(empty), cta = if (cta) stringResource(R.string.ds_empty_cta) else null, onCta = onBook)
        return
    }
    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(list, key = { it.bookingId }) { card(it) }
    }
}

private sealed interface Mode {
    data object Upcoming : Mode
    data object Watched : Mode
    data class Cancelled(val item: CancelledTicket) : Mode
}

@Composable
private fun TicketCard(group: TicketGroup, mode: Mode, ratings: Map<String, Int>, onRate: (String, Int) -> Unit, onClick: () -> Unit) {
    val c = Ceta.colors
    val t = group.lead
    val alpha = when (mode) { Mode.Upcoming -> 1f; Mode.Watched -> 0.8f; is Mode.Cancelled -> 0.6f }
    Column(Modifier.fillMaxWidth().alpha(alpha).cardSurface(RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Poster(t.posterUrl, t.movieTitle, Modifier.size(width = 64.dp, height = 92.dp).clip(RoundedCornerShape(14.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(t.movieTitle, color = c.tx, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(t.cinemaName.orEmpty(), color = c.mu, fontSize = 13.sp)
                Text(Formatters.date(Formatters.localDate(t.start)) + " · " + Formatters.time(t.start), color = c.tx, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (mode == Mode.Upcoming) WhenChip(t.start)
                    Chip(group.seats.joinToString(", "), c.ac, c.acs)
                    Chip(if (group.allUsed) stringResource(R.string.ds_used) else t.format.label, c.tx, c.sf2)
                }
            }
            Icon(
                if (mode == Mode.Upcoming) Icons.Rounded.QrCode2 else Icons.Rounded.ChevronRight, null, tint = c.mu,
                modifier = Modifier.size(if (mode == Mode.Upcoming) 26.dp else 24.dp),
            )
        }
        when (mode) {
            Mode.Watched -> { Divider(); RatingRow(t.code, ratings[t.code], onRate) }
            is Mode.Cancelled -> { Divider(); RefundRow(mode.item.refund) }
            Mode.Upcoming -> Unit
        }
    }
}

@Composable
private fun Divider() = HorizontalDivider(color = Ceta.colors.ln, modifier = Modifier.padding(top = 10.dp))

@Composable
private fun Chip(text: String, fg: Color, bg: Color) {
    Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp))
}

/** Today / Tomorrow / In n days: a solid accent chip on an upcoming ticket. */
@Composable
private fun WhenChip(start: java.time.Instant) {
    val now = remember { java.time.Instant.now() }
    val label = when (val w = TicketGrouping.whenIs(start, now)) {
        ShowWhen.Today -> stringResource(R.string.when_today)
        ShowWhen.Tomorrow -> stringResource(R.string.when_tomorrow)
        is ShowWhen.InDays -> stringResource(R.string.when_days, w.days.toInt())
    }
    Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Ceta.colors.ac).padding(horizontal = 8.dp, vertical = 3.dp))
}

/** Five stars right in the list; the choice is kept by ticket code. */
@Composable
private fun RatingRow(code: String, rating: Int?, onRate: (String, Int) -> Unit) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (rating == null) stringResource(R.string.rate_prompt) else stringResource(R.string.rate_done, rating),
            color = c.mu, fontSize = 13.sp, modifier = Modifier.weight(1f),
        )
        (1..5).forEach { n ->
            val label = stringResource(R.string.rate_star, n)
            Box(Modifier.size(width = 36.dp, height = 44.dp).clickable { onRate(code, n) }.semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Star, null, tint = if (rating != null && n <= rating) StarGold else c.ln2, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun RefundRow(refund: Long) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.CurrencyExchange, null, tint = c.mu, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.refunded_points, MoneyFormatter.format(refund)), color = c.mu, fontSize = 13.sp)
    }
}
