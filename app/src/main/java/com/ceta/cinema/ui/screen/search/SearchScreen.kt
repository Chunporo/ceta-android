package com.ceta.cinema.ui.screen.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.domain.model.Movie
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.theme.Ceta

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(onBack: () -> Unit, onMovie: (String) -> Unit, vm: SearchViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val c = Ceta.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, stringResource(R.string.back), tint = c.tx) }
            Row(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(16.dp)).background(c.sf).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = c.mu)
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = s.query, onValueChange = vm::onQuery, singleLine = true, cursorBrush = SolidColor(c.ac),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.tx),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { vm.remember() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    decorationBox = { inner ->
                        if (s.query.isEmpty() && s.genre == null) Text(stringResource(R.string.ds_search_ph), color = c.mu, style = MaterialTheme.typography.bodyLarge)
                        else if (s.query.isEmpty()) Text(s.genre?.name.orEmpty(), color = c.tx, style = MaterialTheme.typography.bodyLarge)
                        inner()
                    },
                )
            }
        }
        when {
            s.error != null -> ErrorBox(s.error!!, null)
            s.idle -> IdleContent(s, vm)
            s.loading && s.results == null -> LoadingBox()
            s.results.orEmpty().isEmpty() -> if (s.results != null) EmptyState(stringResource(R.string.ds_no_results), Modifier.padding(top = 48.dp))
            else -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(s.results.orEmpty(), key = { it.id }) { m -> ResultRow(m) { vm.remember(); onMovie(m.id) } }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun IdleContent(s: SearchUiState, vm: SearchViewModel) {
    val c = Ceta.colors
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (s.recents.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.ds_recent), color = c.mu, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.clear_all), color = c.ac, style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable { vm.clearRecents() })
                }
            }
            items(s.recents, key = { it }) { r ->
                Row(Modifier.fillMaxWidth().clickable { vm.pickRecent(r) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, null, tint = c.mu, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(r, color = c.tx, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (s.genres.isNotEmpty()) {
            item { Text(stringResource(R.string.ds_genres), color = c.mu, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp)) }
            item {
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.genres.forEach { g -> FilterChip(g.name, selected = false, onClick = { vm.pickGenre(g) }) }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(movie: Movie, onClick: () -> Unit) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Poster(movie.posterUrl, movie.id, Modifier.width(56.dp).aspectRatio(2f / 3f))
        Column(Modifier.weight(1f)) {
            Text(movie.title, color = c.tx, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(movie.genres.joinToString(" · ").ifEmpty { null }, movie.durationMin?.let { stringResource(R.string.duration_min, it) }).joinToString(" · "),
                color = c.mu, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AgeBadge(movie.ageRating)
    }
}
