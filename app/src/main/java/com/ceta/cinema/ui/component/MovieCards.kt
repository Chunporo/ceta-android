package com.ceta.cinema.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.StarHalf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.domain.model.Movie
import com.ceta.cinema.ui.theme.Ceta

/** A poster tile of the home grid: age badge on the left, score (or release day for coming soon) on the right. */
@Composable
fun PosterCard(movie: Movie, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ceta.colors
    Column(modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            Poster(movie.posterUrl, movie.id, Modifier.fillMaxSize())
            AgeBadge(movie.ageRating, Modifier.align(Alignment.TopStart).padding(8.dp))
            val badge = if (movie.isNowShowing) movie.ratingAverage?.let { "★ ${Formatters.score(it)}" }
                else movie.releaseDate?.let { "%02d/%02d".format(it.dayOfMonth, it.monthValue) }
            if (badge != null) {
                Text(badge, color = Color.White, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(movie.title, color = c.tx, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(movie.genres.joinToString(" · "), color = c.mu, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Half-star rating: 1..10 where 10 is five full stars. Tapping the left half of a star gives the odd value. */
@Composable
fun StarRating(value: Int, modifier: Modifier = Modifier, onChange: ((Int) -> Unit)? = null, size: Int = 36) {
    val c = Ceta.colors
    Row(modifier) {
        (1..5).forEach { star ->
            val full = value >= star * 2
            val half = !full && value == star * 2 - 1
            Box(Modifier.size(size.dp)) {
                Icon(
                    when { full -> Icons.Rounded.Star; half -> Icons.Rounded.StarHalf; else -> Icons.Rounded.StarBorder },
                    null, tint = if (full || half) Color(0xFFF2B53B) else c.ln2, modifier = Modifier.fillMaxSize(),
                )
                if (onChange != null) Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { onChange(star * 2 - 1) })
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { onChange(star * 2) })
                }
            }
        }
    }
}
