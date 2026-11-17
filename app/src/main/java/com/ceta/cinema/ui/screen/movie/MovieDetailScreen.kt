package com.ceta.cinema.ui.screen.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.ErrorMapper
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.MoneyFormatter
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.Movie
import com.ceta.cinema.domain.model.Review
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.theme.Ceta

@Composable
fun MovieDetailScreen(onBack: () -> Unit, onBook: (String) -> Unit, onLogin: () -> Unit, vm: MovieDetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    when (val s = state) {
        UiState.Loading -> LoadingBox(Modifier.background(c.bg))
        is UiState.Error -> Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) { CetaTopBar("", onBack); ErrorBox(s.error, vm::load) }
        is UiState.Empty -> Unit
        is UiState.Content -> DetailContent(s.data, vm, onBack, onBook, onLogin)
    }
}

@Composable
private fun DetailContent(ui: MovieDetailUi, vm: MovieDetailViewModel, onBack: () -> Unit, onBook: (String) -> Unit, onLogin: () -> Unit) {
    val c = Ceta.colors
    val movie = ui.movie
    val snackbar = remember { SnackbarHostState() }
    var showReview by rememberSaveable { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    val problem = ui.problem?.let { stringResource(ErrorMapper.messageFor(it)) }
    val loginNeeded = stringResource(R.string.login_needed)
    LaunchedEffect(ui.problem, ui.needsLogin) {
        when {
            ui.needsLogin -> { if (snackbar.showSnackbar(loginNeeded, actionLabel = null) == SnackbarResult.Dismissed) Unit; onLogin(); vm.consumeProblem() }
            problem != null -> { snackbar.showSnackbar(problem); vm.consumeProblem() }
        }
    }
    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Header(movie, onBack, onTrailer = { movie.trailerUrl?.let(uri::openUri) })
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(movie.title, style = MaterialTheme.typography.headlineMedium, color = c.tx)
                    StatsRow(movie)
                    Synopsis(movie.description)
                    People(movie)
                    Reviews(ui, onWrite = { if (vm.requestReview()) showReview = true })
                    Spacer(Modifier.height(8.dp))
                }
            }
            BottomBar(ui, onBook = { onBook(movie.id) }, onRemind = vm::toggleReminder)
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }
    if (showReview) ReviewDialog(
        existing = ui.myReview, busy = ui.busy,
        onSubmit = { rating, comment -> vm.saveReview(rating, comment); showReview = false },
        onDelete = { vm.deleteReview(); showReview = false },
        onDismiss = { showReview = false },
    )
}

@Composable
private fun Header(movie: Movie, onBack: () -> Unit, onTrailer: () -> Unit) {
    Box(Modifier.fillMaxWidth().aspectRatio(16f / 11f)) {
        Poster(movie.posterUrl, movie.id, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Ceta.colors.bg)))
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0x66000000)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, stringResource(R.string.back), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (!movie.trailerUrl.isNullOrBlank()) Row(
                Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x66000000)).clickable(onClick = onTrailer).padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.ds_trailer), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
        Row(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AgeBadge(movie.ageRating)
            movie.durationMin?.let { Text(stringResource(R.string.duration_min, it), color = Ceta.colors.tx, style = MaterialTheme.typography.labelMedium) }
            Text(movie.genres.joinToString(" · "), color = Ceta.colors.mu, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatsRow(movie: Movie) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.sf).padding(vertical = 14.dp)) {
        Stat(Modifier.weight(1f), if (movie.ratingAverage != null) "★ ${Formatters.score(movie.ratingAverage)}" else "—",
            if (movie.ratingCount > 0) "${Formatters.votes(movie.ratingCount)} ${stringResource(R.string.ds_votes)}" else stringResource(R.string.movie_not_rated))
        Stat(Modifier.weight(1f), movie.releaseDate?.let(Formatters::date) ?: "—", stringResource(R.string.ds_release))
        Stat(Modifier.weight(1f), movie.language ?: "—", stringResource(R.string.ds_language))
    }
}

@Composable
private fun Stat(modifier: Modifier, value: String, label: String) {
    val c = Ceta.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = c.tx, style = MaterialTheme.typography.titleSmall)
        Text(label, color = c.mu, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Synopsis(text: String?) {
    if (text.isNullOrBlank()) return
    val c = Ceta.colors
    var open by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(stringResource(R.string.ds_synopsis), color = c.tx, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(text, color = c.mu, style = MaterialTheme.typography.bodyMedium, maxLines = if (open) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
        Text(stringResource(if (open) R.string.ds_less else R.string.ds_more), color = c.ac, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 6.dp).clickable { open = !open })
    }
}

@Composable
private fun People(movie: Movie) {
    if (movie.director.isNullOrBlank() && movie.cast.isEmpty()) return
    val c = Ceta.colors
    val people = listOfNotNull(movie.director?.takeIf { it.isNotBlank() }?.let { it to R.string.ds_director }) + movie.cast.map { it to R.string.ds_actor }
    Column {
        Text(stringResource(R.string.ds_cast), color = c.tx, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(people.size) { i ->
                val (name, role) = people[i]
                Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(c.sf2), contentAlignment = Alignment.Center) {
                        Text(name.split(' ').mapNotNull { it.firstOrNull() }.takeLast(2).joinToString("").uppercase(), color = c.tx, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(name, color = c.tx, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(role), color = c.mu, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun Reviews(ui: MovieDetailUi, onWrite: () -> Unit) {
    val c = Ceta.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.reviews_title), color = c.tx, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(stringResource(if (ui.myReview != null) R.string.review_edit else R.string.review_write), color = c.ac,
                style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable(onClick = onWrite))
        }
        if (ui.reviews.isEmpty()) Text(stringResource(R.string.reviews_empty), color = c.mu, style = MaterialTheme.typography.bodyMedium)
        ui.reviews.forEach { ReviewRow(it) }
    }
}

@Composable
private fun ReviewRow(review: Review) {
    val c = Ceta.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (review.mine) c.acs else c.sf).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (review.mine) stringResource(R.string.review_mine) else review.author, color = c.tx, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            StarRating(review.rating, size = 16)
        }
        if (!review.comment.isNullOrBlank()) Text(review.comment, color = c.mu, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BottomBar(ui: MovieDetailUi, onBook: () -> Unit, onRemind: () -> Unit) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth().background(c.sf).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (ui.movie.isNowShowing) {
            ui.movie.fromPrice?.let { price ->
                Column(Modifier.padding(end = 16.dp)) {
                    Text(stringResource(R.string.ds_from_price), color = c.mu, style = MaterialTheme.typography.bodySmall)
                    Text(MoneyFormatter.format(price), color = c.tx, style = MaterialTheme.typography.titleMedium)
                }
            }
            PrimaryButton(stringResource(R.string.ds_book_now), onBook, Modifier.weight(1f))
        } else {
            OutlineButton(stringResource(if (ui.reminderOn) R.string.ds_reminded else R.string.ds_remind), onRemind, Modifier.weight(1f), enabled = !ui.busy,
                icon = if (ui.reminderOn) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone)
        }
    }
}

@Composable
private fun ReviewDialog(existing: Review?, busy: Boolean, onSubmit: (Int, String) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val c = Ceta.colors
    var rating by rememberSaveable { mutableIntStateOf(existing?.rating ?: 8) }
    var comment by rememberSaveable { mutableStateOf(existing?.comment.orEmpty()) }
    // The dialog is its own window; resolve the words here, in the app's chosen language, and hand it plain text.
    val title = stringResource(if (existing != null) R.string.review_edit else R.string.review_write)
    val yourRating = stringResource(R.string.review_your_rating)
    val hint = stringResource(R.string.review_comment_hint)
    val submit = stringResource(R.string.review_submit)
    val delete = stringResource(R.string.review_delete)
    val cancel = stringResource(R.string.cancel)
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.sf,
        title = { Text(title, color = c.tx) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(yourRating, color = c.mu, style = MaterialTheme.typography.labelMedium)
                StarRating(rating, onChange = { rating = it })
                OutlinedTextField(comment, { comment = it.take(1000) }, placeholder = { Text(hint) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            }
        },
        confirmButton = { TextButton({ onSubmit(rating, comment) }, enabled = !busy) { Text(submit, color = c.ac) } },
        dismissButton = {
            Row {
                if (existing != null) TextButton(onDelete, enabled = !busy) { Text(delete, color = c.mu) }
                TextButton(onDismiss) { Text(cancel, color = c.mu) }
            }
        },
    )
}
