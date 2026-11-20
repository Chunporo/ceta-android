package com.ceta.cinema.ui.screen.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.data.repository.Trend
import com.ceta.cinema.data.repository.TrendingItem
import com.ceta.cinema.domain.model.Cinema
import com.ceta.cinema.domain.usecase.MovieHit
import com.ceta.cinema.domain.usecase.NextShowtimes
import com.ceta.cinema.domain.usecase.SearchFilter
import com.ceta.cinema.domain.usecase.SearchResults
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface
import java.time.format.DateTimeFormatter

private val Up = Color(0xFF3FA66B)

/**
 * Search (design item 16): recent searches, what is trending, genre tiles; then, while typing, filter chips with counts and result
 * cards for movies (with today's next showtimes) and cinemas, or a "no results" page with suggestions.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit, onMovie: (String) -> Unit, onShow: (Long) -> Unit, onShowtimes: (String) -> Unit, onCinema: (Long) -> Unit,
    vm: SearchViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    val c = Ceta.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.sf).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, stringResource(R.string.back), tint = c.tx, modifier = Modifier.size(22.dp))
            }
            Row(
                Modifier.weight(1f).height(48.dp).cardSurface(RoundedCornerShape(24.dp)).border(1.5.dp, c.ac, RoundedCornerShape(24.dp)).padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = c.mu, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = s.query, onValueChange = vm::onQuery, singleLine = true, cursorBrush = SolidColor(c.ac),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.tx),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { vm.remember() }),
                    modifier = Modifier.weight(1f).focusRequester(focus),
                    decorationBox = { inner ->
                        if (s.query.isEmpty()) Text(stringResource(R.string.search_ph), color = c.mu, style = MaterialTheme.typography.bodyLarge)
                        inner()
                    },
                )
                if (s.query.isNotEmpty()) {
                    val label = stringResource(R.string.search_clear)
                    Box(Modifier.size(36.dp).clip(CircleShape).background(c.sf2).clickable { vm.onQuery("") }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, label, tint = c.mu, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        when {
            s.error != null -> ErrorBox(s.error!!, vm::retry)
            s.loading -> LoadingBox()
            s.idle -> Idle(s, vm, onMovie = { vm.remember(); onMovie(it) })
            else -> Typing(s, vm, onMovie = { vm.remember(); onMovie(it) }, onShow = { vm.remember(); onShow(it) }, onShowtimes = { vm.remember(); onShowtimes(it) }, onCinema = { vm.remember(); onCinema(it) })
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Ceta.colors.tx, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = modifier)
}

// ---------------------------------------------------------------- nothing typed

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Idle(s: SearchUiState, vm: SearchViewModel, onMovie: (String) -> Unit) {
    val c = Ceta.colors
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (s.recents.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(stringResource(R.string.ds_recent), Modifier.weight(1f))
                    Text(stringResource(R.string.search_clear_all), color = c.mu, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { vm.clearRecents() }.padding(4.dp))
                }
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    s.recents.forEach { r ->
                        Row(Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(c.sf2), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.fillMaxHeight().clickable { vm.pickRecent(r) }.padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.History, null, tint = c.mu, modifier = Modifier.size(18.dp))
                                Text(r, color = c.tx, fontSize = 15.sp)
                            }
                            val remove = stringResource(R.string.search_remove_recent, r)
                            Box(Modifier.size(38.dp).clickable { vm.removeRecent(r) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Close, remove, tint = c.mu, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
        if (s.trending.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.search_trending), Modifier.padding(top = 4.dp)) }
            itemsIndexedTrending(s.trending, onMovie)
        }
        if (s.genres.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.ds_genres), Modifier.padding(top = 8.dp)) }
            items(s.genres.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { g -> GenreTile(g, Modifier.weight(1f)) { vm.pickGenre(g.genre) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexedTrending(list: List<TrendingItem>, onMovie: (String) -> Unit) {
    items(list.size) { i -> TrendingRow(i + 1, list[i], onMovie) }
}

@Composable
private fun TrendingRow(rank: Int, item: TrendingItem, onMovie: (String) -> Unit) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth().clickable { onMovie(item.movie.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(rank.toString(), color = if (rank <= 3) c.ac else c.mu, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
        Poster(item.movie.posterUrl, item.movie.id, Modifier.size(width = 40.dp, height = 58.dp).clip(RoundedCornerShape(10.dp)))
        Column(Modifier.weight(1f)) {
            Text(item.movie.title, color = c.tx, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.movie.genres.joinToString(" · "), color = c.mu, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        when (item.trend) {
            Trend.UP -> Icon(Icons.Rounded.TrendingUp, null, tint = Up, modifier = Modifier.size(24.dp))
            Trend.FLAT -> Icon(Icons.Rounded.TrendingFlat, null, tint = c.mu, modifier = Modifier.size(24.dp))
            Trend.DOWN -> Icon(Icons.Rounded.TrendingDown, null, tint = c.ac, modifier = Modifier.size(24.dp))
        }
    }
}

/** A genre tile: a gradient of the genre's own hue, a big faint icon, two tilted posters, the icon tile, the name and the count. */
@Composable
private fun GenreTile(card: GenreCard, modifier: Modifier, onClick: () -> Unit) {
    val hue = (card.genre.slug.hashCode().toLong().and(0x7fffffff) % 360).toFloat()
    val icon = genreIcon(card.genre.slug)
    Box(
        modifier.height(104.dp).clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color.hsv(hue, 0.62f, 0.56f), Color.hsv(hue, 0.7f, 0.36f)))).clickable(onClick = onClick),
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.10f), modifier = Modifier.size(96.dp).align(Alignment.BottomEnd).offset(x = 16.dp, y = 16.dp))
        Row(Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp)) {
            card.posters.take(2).forEachIndexed { i, url ->
                Poster(url, card.genre.slug + i, Modifier.padding(start = if (i == 0) 0.dp else 0.dp).offset(x = if (i == 1) (-14).dp else 0.dp).size(width = 36.dp, height = 52.dp).rotate(if (i == 0) -8f else 8f).clip(RoundedCornerShape(8.dp)))
            }
        }
        Box(Modifier.padding(start = 14.dp, top = 12.dp).size(30.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(0.62f).padding(start = 14.dp, bottom = 10.dp)) {
            Text(card.genre.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.search_movies_count, card.count), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
    }
}

private fun genreIcon(slug: String): ImageVector = when {
    "hanh-dong" in slug || "action" in slug -> Icons.Rounded.LocalFireDepartment
    "kinh-di" in slug || "horror" in slug -> Icons.Rounded.Mood
    "tinh-cam" in slug || "romance" in slug || "lang-man" in slug -> Icons.Rounded.Favorite
    "hoat-hinh" in slug || "animation" in slug -> Icons.Rounded.AutoAwesome
    "hai" in slug || "comedy" in slug -> Icons.Rounded.SentimentVerySatisfied
    "vien-tuong" in slug || "sci" in slug || "khoa-hoc" in slug -> Icons.Rounded.RocketLaunch
    "gia-dinh" in slug || "family" in slug -> Icons.Rounded.FamilyRestroom
    "phieu-luu" in slug || "adventure" in slug -> Icons.Rounded.Explore
    "hinh-su" in slug || "crime" in slug || "bi-an" in slug || "thriller" in slug -> Icons.Rounded.Search
    else -> Icons.Rounded.Theaters
}

// ---------------------------------------------------------------- typing

@Composable
private fun Typing(
    s: SearchUiState, vm: SearchViewModel,
    onMovie: (String) -> Unit, onShow: (Long) -> Unit, onShowtimes: (String) -> Unit, onCinema: (Long) -> Unit,
) {
    val results = s.results ?: return
    if (results.count(SearchFilter.ALL) == 0) { NoResults(s, vm); return }
    Column(Modifier.fillMaxSize()) {
        FilterChips(results, s.filter, vm::setFilter)
        val movies = when (s.filter) { SearchFilter.NOW -> results.now; SearchFilter.SOON -> results.soon; SearchFilter.ALL -> results.movies; SearchFilter.CINEMA -> emptyList() }
        val cinemas = if (s.filter == SearchFilter.ALL || s.filter == SearchFilter.CINEMA) results.cinemas else emptyList()
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (movies.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.search_section_movies), Modifier.padding(top = 4.dp)) }
                items(movies, key = { it.movie.id }) { hit -> MovieCard(hit, s.shows[hit.movie.id], onMovie, onShow, onShowtimes) }
            }
            if (cinemas.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.search_section_cinemas), Modifier.padding(top = 8.dp)) }
                item { CinemaList(cinemas, onCinema) }
            }
        }
    }
}

@Composable
private fun FilterChips(results: SearchResults, selected: SearchFilter, onSelect: (SearchFilter) -> Unit) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            SearchFilter.ALL to R.string.search_filter_all, SearchFilter.NOW to R.string.search_filter_now,
            SearchFilter.SOON to R.string.search_filter_soon, SearchFilter.CINEMA to R.string.search_filter_cinema,
        ).forEach { (filter, label) ->
            val on = filter == selected
            Row(
                Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(if (on) c.ac else Color.Transparent)
                    .border(if (on) BorderStroke(0.dp, Color.Transparent) else BorderStroke(1.5.dp, c.ln2), RoundedCornerShape(18.dp))
                    .clickable { onSelect(filter) }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(label), color = if (on) Color.White else c.tx, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(results.count(filter).toString(), color = if (on) Color.White.copy(alpha = 0.85f) else c.mu, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieCard(hit: MovieHit, next: NextShowtimes?, onMovie: (String) -> Unit, onShow: (Long) -> Unit, onShowtimes: (String) -> Unit) {
    val c = Ceta.colors
    val m = hit.movie
    Row(
        Modifier.fillMaxWidth().cardSurface(RoundedCornerShape(24.dp)).clickable { onMovie(m.id) }.padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Poster(m.posterUrl, m.id, Modifier.size(width = 76.dp, height = 112.dp).clip(RoundedCornerShape(14.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AgeBadge(m.ageRating)
                Text(m.title, color = c.tx, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            Text(
                listOfNotNull(m.genres.joinToString(" · ").ifEmpty { null }, m.durationMin?.let { stringResource(R.string.duration_min, it) }).joinToString(" · "),
                color = c.mu, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val line = when {
                hit.person != null -> stringResource(R.string.search_has_person, hit.person)
                else -> listOfNotNull(m.ratingAverage?.let { "★ %.1f".format(it) }, next?.cinemaName).joinToString(" · ").ifEmpty { null }
            }
            if (line != null) Text(line, color = c.mu, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (m.isNowShowing) {
                FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    next?.chips?.forEach { show ->
                        Box(
                            Modifier.height(34.dp).clip(RoundedCornerShape(12.dp)).border(1.5.dp, c.ln2, RoundedCornerShape(12.dp)).clickable { onShow(show.id) }.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(Formatters.time(show.start), color = c.tx, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    }
                    Box(
                        Modifier.height(34.dp).clip(RoundedCornerShape(12.dp)).background(c.acs).clickable { onShowtimes(m.id) }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.search_other_shows), color = c.ac, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
            } else m.releaseDate?.let {
                Text(stringResource(R.string.search_release, it.format(DateTimeFormatter.ofPattern("dd/MM"))), color = c.ac2, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun CinemaList(cinemas: List<Cinema>, onCinema: (Long) -> Unit) {
    val c = Ceta.colors
    Column(Modifier.fillMaxWidth().cardSurface(RoundedCornerShape(24.dp)).padding(vertical = 6.dp)) {
        cinemas.forEachIndexed { i, cinema ->
            Row(Modifier.fillMaxWidth().clickable { onCinema(cinema.id) }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(c.acs), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Theaters, null, tint = c.ac, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(cinema.name, color = c.tx, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(cinema.address, color = c.mu, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = c.mu, modifier = Modifier.size(22.dp))
            }
            if (i < cinemas.lastIndex) androidx.compose.material3.HorizontalDivider(color = c.ln, modifier = Modifier.padding(horizontal = 14.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoResults(s: SearchUiState, vm: SearchViewModel) {
    val c = Ceta.colors
    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        RingIcon(Icons.Rounded.SearchOff, outer = 72.dp, inner = 52.dp, glyph = 28.dp)
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.search_no_results, s.query.trim()), color = c.tx, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.search_no_results_hint), color = c.mu, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            s.genres.take(3).forEach { g ->
                Text(
                    g.genre.name, color = c.tx, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(c.sf2).clickable { vm.pickGenre(g.genre) }.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
    }
}
