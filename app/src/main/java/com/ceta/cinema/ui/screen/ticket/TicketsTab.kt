package com.ceta.cinema.ui.screen.ticket

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.usecase.TicketGroup
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.screen.home.OfflineBanner
import com.ceta.cinema.ui.theme.Ceta

@Composable
fun TicketsTab(onTicket: (Long) -> Unit, onBook: () -> Unit, onSignIn: () -> Unit, vm: TicketsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val signedIn by vm.signedIn.collectAsState()
    val offline by vm.offline.collectAsState()
    val c = Ceta.colors
    var segment by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        Text(stringResource(R.string.ds_tickets_title), style = MaterialTheme.typography.titleLarge, color = c.tx, modifier = Modifier.padding(20.dp))
        if (signedIn != true) {
            EmptyState(stringResource(R.string.login_needed), cta = stringResource(R.string.ds_login), onCta = onSignIn)
            return
        }
        if (offline) OfflineBanner()
        SegmentedTabs(listOf(stringResource(R.string.ds_upcoming), stringResource(R.string.ds_watched)), segment, { segment = it }, Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(12.dp))
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.error, onRetry = { vm.load(forceRefresh = true) })
            is UiState.Empty -> Unit
            is UiState.Content -> {
                val list = if (segment == 0) s.data.upcoming else s.data.watched
                if (list.isEmpty()) EmptyState(stringResource(R.string.ds_empty_tickets), cta = if (segment == 0) stringResource(R.string.ds_empty_cta) else null, onCta = onBook)
                else LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(list, key = { it.bookingId }) { TicketCard(it, past = segment == 1) { onTicket(it.bookingId) } }
                }
            }
        }
    }
}

@Composable
private fun TicketCard(group: TicketGroup, past: Boolean, onClick: () -> Unit) {
    val c = Ceta.colors
    val t = group.lead
    Row(
        Modifier.fillMaxWidth().alpha(if (past) 0.7f else 1f).clip(RoundedCornerShape(20.dp)).background(c.sf).clickable(onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Poster(t.posterUrl, t.movieTitle, Modifier.width(72.dp).aspectRatio(2f / 3f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(t.movieTitle, color = c.tx, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(t.cinemaName.orEmpty(), color = c.mu, style = MaterialTheme.typography.bodySmall)
            Text(Formatters.date(Formatters.localDate(t.start)) + " · " + Formatters.time(t.start), color = c.tx, style = MaterialTheme.typography.bodyMedium)
            Text(group.seats.joinToString(", ") + " · " + t.format.label, color = c.mu, style = MaterialTheme.typography.bodySmall)
            if (group.allUsed) Text(stringResource(R.string.ds_used), color = c.ac, style = MaterialTheme.typography.labelSmall)
        }
    }
}
