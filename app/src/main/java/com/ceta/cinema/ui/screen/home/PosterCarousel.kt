package com.ceta.cinema.ui.screen.home

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.domain.model.Movie
import com.ceta.cinema.domain.model.ShowFormat
import com.ceta.cinema.domain.usecase.FeaturedTag
import com.ceta.cinema.domain.usecase.NextShowtimes
import com.ceta.cinema.ui.component.Poster
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface
import com.ceta.cinema.ui.theme.ageColor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val STEP = 240.dp
private val CARD_H = 350.dp
private const val AUTOPLAY_MS = 4_500L
private const val IDLE_MS = 7_000L

/**
 * The home carousel (design item 1): posters step 240dp apart and snap to the centre; the farther a card is from the centre the
 * smaller, lower and fainter it is, all driven by the continuous scroll position, never by the page number. A blurred copy of
 * each poster cross-fades behind it. Below sit the title, the facts and today's next showtimes of the centred movie.
 */
@Composable
fun PosterCarousel(
    movies: List<Movie>, nextShows: Map<String, NextShowtimes>, onCentered: (Movie) -> Unit,
    onMovie: (String) -> Unit, onBook: (String) -> Unit, onShow: (Long) -> Unit,
) {
    if (movies.isEmpty()) return
    val c = Ceta.colors
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { movies.size }
    val sidePad = ((LocalConfiguration.current.screenWidthDp.dp - STEP) / 2).coerceAtLeast(0.dp)
    var lastTouch by remember { mutableLongStateOf(0L) }
    fun touched() { lastTouch = SystemClock.elapsedRealtime() }
    suspend fun goTo(page: Int) = pager.animateScrollToPage(page, animationSpec = tween(350))

    val dragged by pager.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged) { touched() }
    // Runs by itself every 4.5 s, pauses while the customer touches it and starts again after 7 s of quiet; the end loops to the start.
    LaunchedEffect(movies.size) {
        while (true) {
            delay(AUTOPLAY_MS)
            if (SystemClock.elapsedRealtime() - lastTouch >= IDLE_MS && !pager.isScrollInProgress) goTo((pager.currentPage + 1) % movies.size)
        }
    }
    val current = movies[pager.currentPage.coerceIn(0, movies.lastIndex)]
    LaunchedEffect(current.id) { onCentered(current) }

    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
            BlurredBackdrop(movies, pager)
            HorizontalPager(
                state = pager, pageSize = androidx.compose.foundation.pager.PageSize.Fixed(STEP), pageSpacing = 0.dp,
                contentPadding = PaddingValues(horizontal = sidePad, vertical = 14.dp), beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                PosterPage(movies[page], page, pager, onTap = {
                    touched()
                    if (page == pager.currentPage) onMovie(movies[page].id) else scope.launch { goTo(page) }
                }, onBook = { onBook(movies[page].id) })
            }
        }
        Info(current, nextShows[current.id], onBook = { onBook(current.id) }, onShow = onShow)
        Dots(movies.size, pager.currentPage) { touched(); scope.launch { goTo(it) } }
    }
}

private fun PagerState.position(): Float = currentPage + currentPageOffsetFraction

/** One blurred poster per movie behind the carousel; its opacity follows how close the movie is to the centre. */
@Composable
private fun BoxScope.BlurredBackdrop(movies: List<Movie>, pager: PagerState) {
    val saturate = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.3f) }) }
    // The layer covers the top 75% of the carousel and fades out at both its edges, so no line shows where the blur ends.
    // matchParentSize: the backdrop follows the carousel's height and never adds to it.
    Box(Modifier.matchParentSize()) {
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().fillMaxHeight(0.75f).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.3f to Color.Black, 0.55f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                },
        ) {
            movies.forEachIndexed { i, m ->
                if (m.posterUrl.isNullOrBlank()) return@forEachIndexed
                AsyncImage(
                    model = m.posterUrl, contentDescription = null, contentScale = ContentScale.Crop, colorFilter = saturate,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = max(0f, 1f - abs(i - pager.position())); scaleX = 1.2f }.blur(46.dp),
                )
            }
        }
    }
}

@Composable
private fun PosterPage(movie: Movie, page: Int, pager: PagerState, onTap: () -> Unit, onBook: () -> Unit) {
    val c = Ceta.colors
    val shape = RoundedCornerShape(26.dp)
    fun d() = min(abs(page - pager.position()), 1f)
    Box(
        Modifier.width(STEP).height(CARD_H + 4.dp).graphicsLayer {
            val d = d()
            val s = 1f - 0.28f * d
            scaleX = s; scaleY = s
            translationY = 14.dp.toPx() * d
            alpha = 1f - 0.55f * d
            // The shadow belongs to the centred card only.
            shadowElevation = if (d < 0.5f) 20.dp.toPx() else 0f
            this.shape = shape; clip = false
            ambientShadowColor = Color.Black.copy(alpha = 0.35f); spotShadowColor = Color.Black.copy(alpha = 0.35f)
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(STEP).height(CARD_H).clip(shape).clickable(onClick = onTap)) {
            Poster(movie.posterUrl, movie.id, Modifier.fillMaxSize())
            FeaturedTag.of(movie)?.let { TagPill(it, Modifier.align(Alignment.TopStart).padding(14.dp)) }
            movie.ageRating?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).clip(RoundedCornerShape(7.dp)).background(ageColor(it)).padding(horizontal = 7.dp, vertical = 3.dp))
            }
            // The "book" button belongs to the centred card: it fades out quickly as the card moves away and only works near the centre.
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(120.dp).graphicsLayer { alpha = max(0f, 1f - 2.5f * d()) }
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
            Row(
                Modifier.align(Alignment.BottomCenter).padding(14.dp).fillMaxWidth().height(48.dp)
                    .graphicsLayer { alpha = max(0f, 1f - 2.5f * d()) }
                    .clip(RoundedCornerShape(24.dp)).background(c.ac)
                    .clickable { if (d() < 0.3f) onBook() },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.ConfirmationNumber, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ds_book_now), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun TagPill(tag: FeaturedTag, modifier: Modifier = Modifier) {
    val c = Ceta.colors
    val (bg, fg, text) = when (tag) {
        FeaturedTag.IMAX -> Triple(Color.White, Color(0xFF17171B), "IMAX")
        FeaturedTag.HOT -> Triple(c.ac, Color.White, stringResource(R.string.home_tag_hot))
        FeaturedTag.EARLY -> Triple(Color.Black.copy(alpha = 0.55f), Color.White, stringResource(R.string.home_tag_early))
    }
    Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.44.sp,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp))
}

@Composable
private fun Info(movie: Movie, next: NextShowtimes?, onBook: () -> Unit, onShow: (Long) -> Unit) {
    val c = Ceta.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Two lines are reserved so the showtimes below do not jump when a long title comes to the centre.
        Box(Modifier.heightIn(min = 56.dp), contentAlignment = Alignment.Center) {
            Text(movie.title, color = c.tx, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.48).sp, lineHeight = 28.sp,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(6.dp))
        val facts = buildList {
            movie.durationMin?.let { add(stringResource(R.string.duration_min, it)) }
            addAll(movie.genres.take(2))
        }.joinToString(" · ")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            movie.ratingAverage?.let { Text("★ ${Formatters.score(it)}", color = c.tx, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            if (facts.isNotEmpty()) Text((if (movie.ratingAverage != null) "· " else "") + facts, color = c.mu, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(14.dp))
        NextShows(next, onBook, onShow)
    }
}

@Composable
private fun NextShows(next: NextShowtimes?, onOtherDays: () -> Unit, onShow: (Long) -> Unit) {
    val c = Ceta.colors
    if (next == null) { Spacer(Modifier.height(48.dp)); return } // still loading: keep the height so the page does not jump
    if (next.none) {
        Box(Modifier.height(44.dp).cardSurface(RoundedCornerShape(22.dp)).clickable(onClick = onOtherDays).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.home_other_days), color = c.tx, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Schedule, null, tint = c.mu, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.home_next_shows, next.cinemaName.orEmpty()), color = c.mu, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        next.chips.forEach { show ->
            Column(
                Modifier.height(48.dp).widthIn(min = 72.dp).cardSurface(RoundedCornerShape(16.dp))
                    .border(1.dp, c.ln2, RoundedCornerShape(16.dp)).clickable { onShow(show.id) }.padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Text(Formatters.time(show.start), color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, style = TextStyle(fontFeatureSettings = "tnum"))
                Text(show.format.label, color = if (show.format == ShowFormat.IMAX) c.ac else c.mu, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Dots(count: Int, active: Int, onClick: (Int) -> Unit) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        repeat(count) { i ->
            val on = i == active
            val width by animateDpAsState(if (on) 20.dp else 6.dp, tween(250), label = "dot")
            val color by animateColorAsState(if (on) c.ac else c.ln, tween(250), label = "dotColor")
            // The touch target is taller than the 6dp dot, as small targets are hard to hit.
            Box(Modifier.height(20.dp).clickable { onClick(i) }, contentAlignment = Alignment.Center) {
                Box(Modifier.height(6.dp).width(width).clip(CircleShape).background(color))
            }
        }
    }
}
