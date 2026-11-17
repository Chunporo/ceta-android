package com.ceta.cinema.ui.screen.cinema

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.Cinema
import com.ceta.cinema.domain.model.Showtime
import com.ceta.cinema.domain.usecase.ShowChip
import com.ceta.cinema.domain.usecase.ShowtimeGrouping
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.screen.home.OfflineBanner
import com.ceta.cinema.ui.theme.Ceta
import java.time.Instant

@Composable
fun CinemasTab(onMovie: (String) -> Unit, onShow: (Long) -> Unit, vm: CinemasViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val offline by vm.offline.collectAsState()
    val c = Ceta.colors
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        Text(stringResource(R.string.ds_cinemas_title), style = MaterialTheme.typography.titleLarge, color = c.tx, modifier = Modifier.padding(20.dp))
        if (offline) OfflineBanner()
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.error, vm::load)
            is UiState.Empty -> EmptyState(stringResource(R.string.ds_no_results))
            is UiState.Content -> {
                val ui = s.data
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ui.cities) { city -> FilterChip(cityLabel(city), ui.city == city, { vm.selectCity(city) }) }
                }
                Spacer(Modifier.height(12.dp))
                LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ui.visible, key = { it.id }) { cinema ->
                        CinemaCard(cinema, open = ui.open == cinema.id, shows = ui.shows[cinema.id], onToggle = { vm.toggle(cinema.id) }, onMovie = onMovie, onShow = onShow)
                    }
                }
            }
        }
    }
}

@Composable
private fun cityLabel(code: String) = when (code) {
    "HCM" -> stringResource(R.string.ds_hcm)
    "HN" -> stringResource(R.string.ds_hn)
    else -> code
}

@Composable
private fun CinemaCard(cinema: Cinema, open: Boolean, shows: UiState<List<Showtime>>?, onToggle: () -> Unit, onMovie: (String) -> Unit, onShow: (Long) -> Unit) {
    val c = Ceta.colors
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.sf)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(cinema.name, color = c.tx, style = MaterialTheme.typography.titleSmall)
                    if (cinema.imax) Text("IMAX", color = c.ac, style = MaterialTheme.typography.labelSmall, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(c.acs).padding(horizontal = 6.dp, vertical = 1.dp))
                }
                Text(cinema.address, color = c.mu, style = MaterialTheme.typography.bodySmall)
            }
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = c.mu)
        }
        if (open) Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (cinema.latitude != null && cinema.longitude != null) {
                Text(stringResource(R.string.ds_directions), color = c.ac, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { runCatching { uri.openUri("geo:${cinema.latitude},${cinema.longitude}?q=${cinema.latitude},${cinema.longitude}(${cinema.name})") } })
            }
            when (shows) {
                null, UiState.Loading -> Text("…", color = c.mu)
                is UiState.Error -> Text(stringResource(com.ceta.cinema.core.ErrorMapper.messageFor(shows.error)), color = c.ac, style = MaterialTheme.typography.bodySmall)
                is UiState.Empty -> Unit
                is UiState.Content -> {
                    val movies = ShowtimeGrouping.byMovie(shows.data, Instant.now())
                    if (movies.isEmpty()) Text(stringResource(R.string.ds_no_shows), color = c.mu, style = MaterialTheme.typography.bodyMedium)
                    movies.forEach { m ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Poster(m.posterUrl, m.movieId ?: m.movieTitle, Modifier.width(52.dp).aspectRatio(2f / 3f).clickable { m.movieId?.let(onMovie) })
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(m.movieTitle, color = c.tx, style = MaterialTheme.typography.titleSmall, modifier = Modifier.clickable { m.movieId?.let(onMovie) })
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    m.chips.take(6).forEach { TimeChip(it) { onShow(it.showtime.id) } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeChip(chip: ShowChip, onClick: () -> Unit) {
    val c = Ceta.colors
    Text(
        Formatters.time(chip.showtime.start), color = c.tx, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.alpha(if (chip.enabled) 1f else 0.35f).clip(RoundedCornerShape(12.dp)).border(1.5.dp, c.ln2, RoundedCornerShape(12.dp))
            .clickable(enabled = chip.enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
