package com.ceta.cinema.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.Movie
import com.ceta.cinema.domain.model.Promotion
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.theme.Ceta
import java.time.ZoneId

@Composable
fun HomeTab(
    onMovie: (String) -> Unit, onBook: (String) -> Unit, onSearch: () -> Unit,
    onNotifications: () -> Unit, onOffers: () -> Unit, onCinemas: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val unread by vm.unread.collectAsState()
    val name by vm.displayName.collectAsState()
    val offline by vm.offline.collectAsState()
    val c = Ceta.colors
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        TopRow(unread, onNotifications, onCinemas)
        if (offline) OfflineBanner()
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.error, vm::load)
            is UiState.Empty -> EmptyState(stringResource(R.string.ds_no_results))
            is UiState.Content -> HomeContent(s.data, name, onMovie, onBook, onSearch, onOffers)
        }
    }
}

@Composable
private fun TopRow(unread: Long, onNotifications: () -> Unit, onCinemas: () -> Unit) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(c.ac), contentAlignment = Alignment.Center) {
            Text("C", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(8.dp))
        Text("CETA", color = c.tx, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.weight(1f))
        Row(Modifier.clip(RoundedCornerShape(20.dp)).background(c.sf).clickable(onClick = onCinemas).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocationOn, null, tint = c.ac, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.ds_hcm), color = c.tx, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.sf).clickable(onClick = onNotifications), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Notifications, stringResource(R.string.notifications), tint = c.tx)
            if (unread > 0) Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(10.dp).clip(CircleShape).background(c.ac))
        }
    }
}

@Composable
fun OfflineBanner() {
    val c = Ceta.colors
    Text(stringResource(R.string.offline_banner), color = c.tx, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.fillMaxWidth().background(c.sf2).padding(horizontal = 20.dp, vertical = 8.dp))
}

@Composable
private fun HomeContent(
    data: HomeData, name: String?, onMovie: (String) -> Unit, onBook: (String) -> Unit, onSearch: () -> Unit, onOffers: () -> Unit,
) {
    val c = Ceta.colors
    var segment by rememberSaveable { mutableIntStateOf(0) }
    val grid = if (segment == 0) data.nowShowing else data.comingSoon
    val heroWidth = LocalConfiguration.current.screenWidthDp.dp * 0.78f
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(if (name.isNullOrBlank()) stringResource(R.string.home_hello_guest) else stringResource(R.string.home_hello, name),
                    style = MaterialTheme.typography.headlineMedium, color = c.tx)
                Text(stringResource(R.string.ds_hello_sub), style = MaterialTheme.typography.bodyLarge, color = c.mu)
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(c.sf).clickable(onClick = onSearch).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = c.mu)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.ds_search_ph), color = c.mu, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (data.featured.isNotEmpty()) item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(data.featured, key = { it.id }) { m ->
                    HeroCard(m, onClick = { onMovie(m.id) }, onBook = { onBook(m.id) }, modifier = Modifier.width(heroWidth).aspectRatio(3f / 4f))
                }
            }
        }
        item {
            SegmentedTabs(listOf(stringResource(R.string.ds_now_showing), stringResource(R.string.ds_coming_soon)), segment, { segment = it },
                Modifier.padding(horizontal = 20.dp))
        }
        if (grid.isEmpty()) item { EmptyState(stringResource(R.string.ds_no_results)) }
        items(grid.chunked(2), key = { row -> row.first().id }) { row -> PosterRow(row, onMovie) }
        if (data.offers.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.ds_promos), action = stringResource(R.string.ds_view_all), onAction = onOffers) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(data.offers, key = { it.id }) { OfferChip(it, onOffers) }
                }
            }
        }
    }
}

@Composable
private fun PosterRow(row: List<Movie>, onMovie: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        row.forEach { PosterCard(it, { onMovie(it.id) }, Modifier.weight(1f)) }
        if (row.size == 1) Spacer(Modifier.weight(1f)) // keep a lone last poster the same width as the others
    }
}

@Composable
private fun OfferChip(promo: Promotion, onClick: () -> Unit) {
    val c = Ceta.colors
    val english = LocalConfiguration.current.locales[0].language == "en"
    Column(Modifier.width(240.dp).clip(RoundedCornerShape(18.dp)).background(c.sf).clickable(onClick = onClick).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(promo.title(english), color = c.tx, style = MaterialTheme.typography.titleSmall, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            promo.validUntil?.let { Text(stringResource(R.string.ds_until) + Formatters.date(Formatters.localDate(it)), color = c.mu, style = MaterialTheme.typography.bodySmall) }
            Text(promo.code, color = c.ac, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.acs).padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}
