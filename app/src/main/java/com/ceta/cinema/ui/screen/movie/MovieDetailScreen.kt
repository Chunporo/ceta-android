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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.SubcomposeAsyncImage
import androidx.compose.ui.layout.ContentScale
import com.ceta.cinema.R
import com.ceta.cinema.core.ErrorMapper
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.MoneyFormatter
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.Movie
import com.ceta.cinema.domain.model.Review
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface

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
    val context = androidx.compose.ui.platform.LocalContext.current
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
                Box(Modifier.fillMaxWidth()) {
                    // Behind everything, scrolling with it: the poster blurred into a soft glow under the content.
                    PosterGlow(movie.posterUrl, Modifier.align(Alignment.TopCenter).offset(y = 170.dp))
                    Column(Modifier.fillMaxWidth()) {
                        Header(movie, onBack, onTrailer = { movie.trailerUrl?.let(uri::openUri) }, onShare = { shareMovie(context, movie) })
                        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(movie.title, color = c.tx, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp, modifier = Modifier.padding(top = 14.dp))
                            StatsRow(movie)
                            Synopsis(movie.description)
                            People(movie)
                            Reviews(ui, onWrite = { if (vm.requestReview()) showReview = true })
                            Spacer(Modifier.height(8.dp))
                        }
                    }
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

private fun shareMovie(context: android.content.Context, movie: Movie) {
    val text = movie.title + (movie.trailerUrl?.let { "\n$it" } ?: "")
    context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text), null))
}

/** The blurred poster behind the movie page (design item 2): 460dp tall from 170dp down, 45% opaque, faded out at top and bottom. */
@Composable
private fun PosterGlow(url: String?, modifier: Modifier = Modifier) {
    if (url.isNullOrBlank()) return
    val saturate = remember { androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(1.3f) }) }
    Box(
        modifier.fillMaxWidth().height(460.dp).graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.2f to Color.Black, 0.45f to Color.Black, 1f to Color.Transparent), blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
            },
    ) {
        coil3.compose.AsyncImage(
            model = url, contentDescription = null, contentScale = ContentScale.Crop, colorFilter = saturate,
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.2f; alpha = 0.45f }.blur(50.dp),
        )
    }
}

/** A backdrop with a centred trailer button, then the poster card overlapping it next to the age badge, length and genres. */
@Composable
private fun Header(movie: Movie, onBack: () -> Unit, onTrailer: () -> Unit, onShare: () -> Unit) {
    val c = Ceta.colors
    Box(Modifier.fillMaxWidth()) {
    Box(Modifier.fillMaxWidth().height(250.dp)) {
        Poster(movie.posterUrl, movie.id, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to c.bg.copy(alpha = 0.55f))))
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x73000000)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBack, stringResource(R.string.back), tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x73000000)).clickable(onClick = onShare), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Share, null, tint = Color.White, modifier = Modifier.size(21.dp))
            }
        }
        if (!movie.trailerUrl.isNullOrBlank()) Row(
            Modifier.align(Alignment.Center).height(52.dp).clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha = 0.92f)).clickable(onClick = onTrailer).padding(start = 14.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF111111), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.ds_trailer), color = Color(0xFF111111), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 194.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Poster(movie.posterUrl, movie.id, Modifier.size(width = 104.dp, height = 156.dp).clip(RoundedCornerShape(18.dp)).border(3.dp, c.bg, RoundedCornerShape(18.dp)))
        Column(Modifier.padding(top = 64.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                AgeBadge(movie.ageRating)
                movie.durationMin?.let { Text(stringResource(R.string.duration_min, it), color = c.tx, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
            Text(movie.genres.joinToString(" · "), color = c.mu, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
    }
}

@Composable
private fun StatsRow(movie: Movie) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Stat(Modifier.weight(1f), if (movie.ratingAverage != null) "★ ${Formatters.score(movie.ratingAverage)}" else "—",
            if (movie.ratingCount > 0) "${Formatters.votes(movie.ratingCount)} ${stringResource(R.string.ds_votes)}" else stringResource(R.string.movie_not_rated), big = true)
        Stat(Modifier.weight(1.35f), movie.releaseDate?.let(Formatters::date) ?: "—", stringResource(R.string.ds_release))
        Stat(Modifier.weight(0.8f), movie.language ?: "—", stringResource(R.string.ds_language))
    }
}

@Composable
private fun Stat(modifier: Modifier, value: String, label: String, big: Boolean = false) {
    val c = Ceta.colors
    Column(modifier.cardSurface(RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(value, color = c.tx, fontSize = if (big) 20.sp else 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(label, color = c.mu, fontSize = 12.sp, maxLines = 1)
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
    if (movie.director.isNullOrBlank() && movie.castMembers.isEmpty()) return
    val c = Ceta.colors
    // (name, line under the name, photo): the director first, then the actors with the character they play when we know it.
    val directorLabel = stringResource(R.string.ds_director)
    val actorLabel = stringResource(R.string.ds_actor)
    val people = listOfNotNull(movie.director?.takeIf { it.isNotBlank() }?.let { Triple(it, directorLabel, null as String?) }) +
        movie.castMembers.map { Triple(it.name, it.role ?: actorLabel, it.photoUrl) }
    Column {
        Text(stringResource(R.string.ds_cast), color = c.tx, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(people.size) { i ->
                val (name, line, photo) = people[i]
                val initials = name.split(' ').mapNotNull { it.firstOrNull() }.takeLast(2).joinToString("").uppercase()
                Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).clip(CircleShape).background(c.sf2), contentAlignment = Alignment.Center) {
                        if (photo == null) Text(initials, color = c.tx, style = MaterialTheme.typography.titleSmall)
                        else SubcomposeAsyncImage(model = photo, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(initials, color = c.mu, style = MaterialTheme.typography.titleSmall) } },
                            error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(initials, color = c.tx, style = MaterialTheme.typography.titleSmall) } })
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(name, color = c.tx, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(line, color = c.mu, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
