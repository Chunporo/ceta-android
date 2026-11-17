package com.ceta.cinema.ui.screen.showtime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Theaters
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.LanguageMode
import com.ceta.cinema.domain.model.ShowFormat
import com.ceta.cinema.domain.model.Showtime
import com.ceta.cinema.domain.usecase.DayPart
import com.ceta.cinema.domain.usecase.ShowChip
import com.ceta.cinema.domain.usecase.ShowtimeGrouping
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.theme.Ceta
import java.time.Instant

@Composable
fun ShowtimePickerScreen(onBack: () -> Unit, onPick: (Long) -> Unit, vm: ShowtimePickerViewModel = hiltViewModel()) {
    val ui by vm.state.collectAsState()
    val c = Ceta.colors
    val english = LocalConfiguration.current.locales[0].language == "en"
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        CetaTopBar(stringResource(R.string.ds_book_now), onBack)
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.week) { day -> FilterChip(Formatters.dayChip(day, english), selected = day == ui.date, onClick = { vm.selectDate(day) }) }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(stringResource(R.string.ds_all), ui.format == null, { vm.selectFormat(null) }) }
                items(ShowFormat.entries) { f -> FilterChip(f.label, ui.format == f, { vm.selectFormat(f) }) }
            }
            ViewToggle(ui.byCinema, vm::toggleView)
        }
        Spacer(Modifier.height(8.dp))
        when (val s = ui.shows) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.error, vm::load)
            is UiState.Empty -> Unit
            is UiState.Content -> {
                val now = Instant.now()
                val shows = ShowtimeGrouping.filter(s.data, ui.format)
                if (shows.isEmpty()) EmptyState(stringResource(R.string.ds_no_shows), Modifier.padding(top = 48.dp))
                else if (ui.byCinema) ByCinema(shows, now, onPick) else ByTime(shows, now, onPick)
            }
        }
    }
}

@Composable
private fun ViewToggle(byCinema: Boolean, onToggle: () -> Unit) {
    val c = Ceta.colors
    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(c.sf).padding(3.dp)) {
        listOf(true to Icons.Rounded.Theaters, false to Icons.Rounded.Schedule).forEach { (value, icon) ->
            val on = byCinema == value
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(if (on) c.ac else androidx.compose.ui.graphics.Color.Transparent)
                .clickable { if (!on) onToggle() }, contentAlignment = Alignment.Center) {
                Icon(icon, stringResource(if (value) R.string.view_by_cinema else R.string.view_by_time), tint = if (on) androidx.compose.ui.graphics.Color.White else c.mu, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ByCinema(shows: List<Showtime>, now: Instant, onPick: (Long) -> Unit) {
    val c = Ceta.colors
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items(ShowtimeGrouping.byCinema(shows, now)) { cinema ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.sf).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(cinema.cinemaName.ifBlank { "CETA" }, color = c.tx, style = MaterialTheme.typography.titleSmall)
                Chips(cinema.chips, onPick)
            }
        }
    }
}

@Composable
private fun ByTime(shows: List<Showtime>, now: Instant, onPick: (Long) -> Unit) {
    val c = Ceta.colors
    val parts = ShowtimeGrouping.byDayPart(shows, now)
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        items(DayPart.entries.filter { parts[it].orEmpty().isNotEmpty() }) { part ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(when (part) { DayPart.MORNING -> R.string.ds_morning; DayPart.AFTERNOON -> R.string.ds_afternoon; DayPart.EVENING -> R.string.ds_evening }),
                    color = c.mu, style = MaterialTheme.typography.labelMedium)
                Chips(parts[part].orEmpty(), onPick, showCinema = true)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(chips: List<ShowChip>, onPick: (Long) -> Unit, showCinema: Boolean = false) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        chips.forEach { TimeChip(it, showCinema) { onPick(it.showtime.id) } }
    }
}

@Composable
private fun TimeChip(chip: ShowChip, showCinema: Boolean, onClick: () -> Unit) {
    val c = Ceta.colors
    val s = chip.showtime
    Column(
        Modifier.alpha(if (chip.enabled) 1f else 0.35f).clip(RoundedCornerShape(14.dp)).border(1.5.dp, c.ln2, RoundedCornerShape(14.dp))
            .clickable(enabled = chip.enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(Formatters.time(s.start), color = c.tx, style = MaterialTheme.typography.titleSmall)
        Text(listOf(s.format.label, stringResource(if (s.languageMode == LanguageMode.DUB) R.string.dub_tag else R.string.sub_tag)).joinToString(" · "),
            color = c.mu, style = MaterialTheme.typography.labelSmall)
        if (showCinema && !s.cinemaName.isNullOrBlank()) Text(s.cinemaName, color = c.mu, style = MaterialTheme.typography.labelSmall)
    }
}
