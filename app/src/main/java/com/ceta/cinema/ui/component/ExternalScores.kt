package com.ceta.cinema.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ceta.cinema.R
import com.ceta.cinema.domain.model.ExternalRating
import com.ceta.cinema.domain.model.ExternalRating.Source
import com.ceta.cinema.ui.theme.Ceta

private val MetaGreen = Color(0xFF66CC33)
private val MetaYellow = Color(0xFFFFCC33)
private val MetaRed = Color(0xFFFF0000)

/**
 * The scores other sites give the film, on one compact line: each site's own logo and its score.
 * TODO(release): these are the sites' real logos (from Wikimedia Commons), used for development only. Get permission or the
 * official brand assets from IMDb, Rotten Tomatoes, Metacritic and TMDB before the app is published.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExternalScores(ratings: List<ExternalRating>, modifier: Modifier = Modifier) {
    if (ratings.isEmpty()) return
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ratings.forEach { ScoreChip(it) }
    }
}

@Composable
private fun ScoreChip(r: ExternalRating) {
    val c = Ceta.colors
    val label = "${r.source.label} ${r.display}"
    Row(
        Modifier.height(34.dp).clip(RoundedCornerShape(12.dp)).background(c.sf).padding(horizontal = 10.dp).semantics(mergeDescendants = true) { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        val (logo, height) = when (r.source) {
            Source.IMDB -> R.drawable.score_imdb to 16.dp
            Source.ROTTEN_TOMATOES -> (if (r.value >= 60) R.drawable.score_fresh else R.drawable.score_rotten) to 18.dp
            Source.METACRITIC -> R.drawable.score_meta to 18.dp
            Source.TMDB -> R.drawable.score_tmdb to 20.dp
        }
        Image(painterResource(logo), null, Modifier.height(height))
        Text(r.display, color = if (r.source == Source.METACRITIC) metaColor(r.value) else c.tx, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** Metacritic's own bands: green from 61, yellow from 40, red below. */
private fun metaColor(v: Double): Color = when { v >= 61 -> MetaGreen; v >= 40 -> MetaYellow; else -> MetaRed }
