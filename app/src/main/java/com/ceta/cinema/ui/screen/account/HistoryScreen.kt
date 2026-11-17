package com.ceta.cinema.ui.screen.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.MoneyFormatter
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.BookingStatus
import com.ceta.cinema.domain.model.BookingSummary
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.theme.Ceta

/** The customer's orders, newest first. A paid one opens its tickets; an unpaid one still holding seats opens checkout. */
@Composable
fun HistoryScreen(onBack: () -> Unit, onOpen: (BookingSummary) -> Unit, vm: HistoryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        CetaTopBar(stringResource(R.string.ds_history), onBack)
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.error, vm::load)
            is UiState.Empty -> EmptyState(stringResource(R.string.history_empty), Modifier.padding(top = 48.dp))
            is UiState.Content -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(s.data, key = { it.id }) { Row(it, onOpen) }
            }
        }
    }
}

@Composable
private fun Row(order: BookingSummary, onOpen: (BookingSummary) -> Unit) {
    val c = Ceta.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.sf).clickable { onOpen(order) }.padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Poster(order.posterUrl, order.movieTitle ?: order.id.toString(), Modifier.width(48.dp).aspectRatio(2f / 3f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(order.movieTitle.orEmpty(), color = c.tx, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(listOfNotNull(order.startTime?.let { Formatters.date(Formatters.localDate(it)) + " " + Formatters.time(it) }, order.seatNames.joinToString(", ").ifEmpty { null }).joinToString(" · "),
                color = c.mu, style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.result_order, order.id), color = c.mu, style = MaterialTheme.typography.labelSmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(MoneyFormatter.format(order.totalAmount), color = c.tx, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(statusLabel(order.status)), color = statusColor(order.status), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun statusLabel(s: BookingStatus) = when (s) {
    BookingStatus.PAID -> R.string.status_paid
    BookingStatus.PENDING -> R.string.status_pending
    BookingStatus.CANCELLED -> R.string.status_cancelled
    BookingStatus.EXPIRED -> R.string.status_expired
    BookingStatus.REFUNDED -> R.string.status_refunded
    BookingStatus.UNKNOWN -> R.string.status_pending
}

private fun statusColor(s: BookingStatus) = when (s) {
    BookingStatus.PAID -> Color(0xFF3FA66B)
    BookingStatus.PENDING, BookingStatus.UNKNOWN -> Color(0xFFF2A93B)
    else -> Color(0xFF9D9DA7)
}
