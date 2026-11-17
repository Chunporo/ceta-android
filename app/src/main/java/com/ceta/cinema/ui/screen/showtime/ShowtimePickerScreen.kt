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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
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
    val title = (ui.shows as? UiState.Content)?.data?.firstNotNullOfOrNull { it.movieTitle } ?: stringResource(R.string.ds_book_now)
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.sf).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, stringResource(R.string.back), tint = c.tx, modifier = Modifier.size(22.dp))
            }
            Text(title, color = c.tx, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LazyRow(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.week) { day -> DateTile(day, day == ui.date, day == ui.week.first(), english) { vm.selectDate(day) } }
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { FormatChip(stringResource(R.string.ds_all), ui.format == null) { vm.selectFormat(null) } }
                items(ShowFormat.entries) { f -> FormatChip(f.label, ui.format == f) { vm.selectFormat(f) } }
            }
            ViewToggle(ui.byCinema, vm::toggleView)
        }
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

/** A day tile: weekday on top ("Hôm nay" for today), the date number big underneath. */
@Composable
private fun DateTile(day: java.time.LocalDate, selected: Boolean, today: Boolean, english: Boolean, onClick: () -> Unit) {
    val c = Ceta.colors
    val fg = if (selected) Color.White else c.tx
    Column(
        Modifier.width(58.dp).height(72.dp).clip(RoundedCornerShape(20.dp)).background(if (selected) c.ac else c.sf).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text(if (today) stringResource(R.string.ds_today) else Formatters.weekdayShort(day, english), color = fg.copy(alpha = 0.85f),
            fontSize = if (today) 10.5.sp else 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
        Text("%02d".format(day.dayOfMonth), color = fg, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun FormatChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = Ceta.colors
    Box(
        Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (selected) c.tx else Color.Transparent)
            .border(1.5.dp, if (selected) c.tx else c.ln2, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (selected) c.bg else c.tx, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ViewToggle(byCinema: Boolean, onToggle: () -> Unit) {
    val c = Ceta.colors
    Row(Modifier.clip(RoundedCornerShape(18.dp)).background(c.sf).padding(3.dp)) {
        listOf(true to Icons.Rounded.Storefront, false to Icons.Rounded.Schedule).forEach { (value, icon) ->
            val on = byCinema == value
            Box(Modifier.width(38.dp).height(30.dp).clip(RoundedCornerShape(15.dp)).background(if (on) c.ac else Color.Transparent)
                .clickable { if (!on) onToggle() }, contentAlignment = Alignment.Center) {
                Icon(icon, stringResource(if (value) R.string.view_by_cinema else R.string.view_by_time), tint = if (on) Color.White else c.mu, modifier = Modifier.size(19.dp))
            }
        }
    }
}

/** One card per cinema; inside it the times are grouped by format and language, four to a row. */
@Composable
private fun ByCinema(shows: List<Showtime>, now: Instant, onPick: (Long) -> Unit) {
    val c = Ceta.colors
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(ShowtimeGrouping.byCinema(shows, now)) { cinema ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(cinema.cinemaName.ifBlank { "CETA" }, color = c.tx, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                val groups = cinema.chips.groupBy { it.showtime.format to it.showtime.languageMode }
                for (format in ShowFormat.entries.reversed()) for ((key, chips) in groups.filterKeys { it.first == format }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            key.first.label + " · " + stringResource(if (key.second == LanguageMode.DUB) R.string.dub_tag else R.string.sub_tag),
                            color = c.mu, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        )
                        chips.chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { TimeButton(it, Modifier.weight(1f)) { onPick(it.showtime.id) } }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeButton(chip: ShowChip, modifier: Modifier, onClick: () -> Unit) {
    val c = Ceta.colors
    val s = chip.showtime
    Column(
        modifier.alpha(if (chip.enabled) 1f else 0.35f).height(52.dp).clip(RoundedCornerShape(14.dp)).background(c.bg)
            .border(1.5.dp, c.ln2, RoundedCornerShape(14.dp)).clickable(enabled = chip.enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
    ) {
        Text(Formatters.time(s.start), color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        seatsLeftText(s)?.let { Text(it, color = if (s.isScarce) c.ac else c.mu, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
    }
}

/** "Sold out", or "12 left" / "12 ghế"; nothing when the server did not say. */
@Composable
private fun seatsLeftText(s: Showtime): String? = when {
    s.availableSeats == null -> null
    s.full -> stringResource(R.string.ds_full)
    else -> "${s.availableSeats} ${stringResource(R.string.ds_seats_left).trim()}"
}

/** Fewer than 20 seats left: shown in the accent colour, as in the design. */
private val Showtime.isScarce: Boolean get() = availableSeats != null && availableSeats < 20

/** The day split into morning / afternoon / evening, one row per show: start → end, format badge, cinema. */
@Composable
private fun ByTime(shows: List<Showtime>, now: Instant, onPick: (Long) -> Unit) {
    val c = Ceta.colors
    val parts = ShowtimeGrouping.byDayPart(shows, now)
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        items(DayPart.entries.filter { parts[it].orEmpty().isNotEmpty() }) { part ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(when (part) { DayPart.MORNING -> Icons.Rounded.WbTwilight; DayPart.AFTERNOON -> Icons.Rounded.LightMode; DayPart.EVENING -> Icons.Rounded.DarkMode }, null, tint = c.mu, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(when (part) { DayPart.MORNING -> R.string.ds_morning; DayPart.AFTERNOON -> R.string.ds_afternoon; DayPart.EVENING -> R.string.ds_evening }),
                        color = c.mu, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                parts[part].orEmpty().forEach { ShowRow(it) { onPick(it.showtime.id) } }
            }
        }
    }
}

@Composable
private fun ShowRow(chip: ShowChip, onClick: () -> Unit) {
    val c = Ceta.colors
    val s = chip.showtime
    val (badgeBg, badgeFg) = when (s.format) { ShowFormat.IMAX -> c.ac2 to Color.White; ShowFormat.THREE_D -> c.acs to c.ac; ShowFormat.TWO_D -> c.sf2 to c.tx }
    Row(
        Modifier.fillMaxWidth().alpha(if (chip.enabled) 1f else 0.35f).clip(RoundedCornerShape(18.dp)).background(c.sf)
            .clickable(enabled = chip.enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.width(64.dp)) {
            Text(Formatters.time(s.start), color = c.tx, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            s.end?.let { Text("→ " + Formatters.time(it), color = c.mu, fontSize = 11.5.sp) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.format.label, color = badgeFg, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(badgeBg).padding(horizontal = 7.dp, vertical = 2.dp))
                Text(s.cinemaName.orEmpty(), color = c.tx, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val filled = s.filled
            if (filled != null) Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(c.sf2)) {
                Box(Modifier.fillMaxWidth(filled.coerceIn(0.04f, 1f)).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(if (s.isScarce) c.ac else c.mu))
            } else Text(stringResource(if (s.languageMode == LanguageMode.DUB) R.string.dub_tag else R.string.sub_tag), color = c.mu, fontSize = 11.5.sp)
        }
        seatsLeftText(s)?.let { Text(it, color = c.mu, fontSize = 11.5.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.width(52.dp)) }
    }
}
