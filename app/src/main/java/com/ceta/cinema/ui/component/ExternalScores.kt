package com.ceta.cinema.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.domain.model.ExternalRating
import com.ceta.cinema.domain.model.ExternalRating.Source
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface

private val ImdbYellow = Color(0xFFF5C518)
private val Fresh = Color(0xFFFA320A)
private val Rotten = Color(0xFF6C9B2B)
private val Stem = Color(0xFF3E8E2F)
private val MetaGreen = Color(0xFF66CC33)
private val MetaYellow = Color(0xFFFFCC33)
private val MetaRed = Color(0xFFFF0000)
private val TmdbGreen = Color(0xFF21D07A)
private val TmdbYellow = Color(0xFFD2D531)
private val TmdbRed = Color(0xFFDB2360)

/**
 * The scores other sites give the film, set out like those sites do (design: "enhance the UI like other websites"): the IMDb yellow
 * tag, a fresh or rotten tomato, Metacritic's coloured square and TMDB's user-score ring. They are drawn here, not shipped as the
 * sites' logos. One card per site, side by side, with the number big and the vote count small.
 */
@Composable
fun ExternalScores(ratings: List<ExternalRating>, modifier: Modifier = Modifier) {
    if (ratings.isEmpty()) return
    val c = Ceta.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ratings.forEach { r -> ScoreCard(r, Modifier.weight(1f).fillMaxHeight()) }
            // Keeps one or two cards from stretching across the whole row.
            repeat((4 - ratings.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
        }
        Text(stringResource(R.string.external_scores_note), color = c.mu, fontSize = 11.sp)
    }
}

@Composable
private fun ScoreCard(r: ExternalRating, modifier: Modifier) {
    val c = Ceta.colors
    val label = "${r.source.label} ${r.display}"
    Column(
        modifier.cardSurface(RoundedCornerShape(16.dp)).padding(vertical = 12.dp, horizontal = 6.dp).semantics(mergeDescendants = true) { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.height(30.dp), contentAlignment = Alignment.Center) {
            when (r.source) {
                Source.IMDB -> Text("IMDb", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(ImdbYellow).padding(horizontal = 6.dp, vertical = 3.dp))
                Source.ROTTEN_TOMATOES -> Tomato(fresh = r.value >= 60)
                Source.METACRITIC -> Text(
                    r.value.toInt().toString(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(metaColor(r.value)).padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Source.TMDB -> TmdbRing(r.value / r.scale)
            }
        }
        // The number: IMDb "7.4/10", Rotten Tomatoes "87%"; Metacritic and TMDB already show theirs in the badge.
        when (r.source) {
            Source.IMDB -> Row(verticalAlignment = Alignment.Bottom) {
                Text(r.display, color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("/10", color = c.mu, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
            Source.ROTTEN_TOMATOES -> Text(r.display, color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Source.METACRITIC -> Text("Metascore", color = c.tx, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            Source.TMDB -> Text("TMDB", color = c.tx, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
        Text(r.votes?.let { Formatters.votes(it) } ?: stringResource(sourceCaption(r.source)), color = c.mu, fontSize = 10.sp, maxLines = 1, softWrap = false)
    }
}

private fun sourceCaption(s: Source): Int = when (s) {
    Source.ROTTEN_TOMATOES -> R.string.score_caption_tomatometer
    Source.METACRITIC -> R.string.score_caption_critics
    else -> R.string.score_caption_users
}

private fun metaColor(v: Double): Color = when { v >= 61 -> MetaGreen; v >= 40 -> MetaYellow; else -> MetaRed }

/** A fresh (>= 60 %) tomato is red with a green stem; a rotten one is a green splat. */
@Composable
private fun Tomato(fresh: Boolean) {
    Canvas(Modifier.size(28.dp)) {
        val s = size.width
        if (fresh) {
            drawCircle(Fresh, s * 0.42f, Offset(s / 2, s * 0.58f))
            val leaf = Path().apply {
                moveTo(s * 0.5f, s * 0.22f); lineTo(s * 0.34f, s * 0.08f); lineTo(s * 0.46f, s * 0.18f); lineTo(s * 0.5f, s * 0.02f)
                lineTo(s * 0.54f, s * 0.18f); lineTo(s * 0.66f, s * 0.08f); close()
            }
            drawPath(leaf, Stem)
        } else {
            // A splat: a green disc with bumps around it.
            val bumps = 8
            for (i in 0 until bumps) {
                val a = Math.PI * 2 * i / bumps
                drawCircle(Rotten, s * 0.17f, Offset((s / 2 + Math.cos(a) * s * 0.3f).toFloat(), (s / 2 + Math.sin(a) * s * 0.3f).toFloat()))
            }
            drawCircle(Rotten, s * 0.33f, Offset(s / 2, s / 2))
        }
    }
}

/** TMDB's user score: the percentage in a dark disc with a ring that fills to it (green, yellow or pink by how high it is). */
@Composable
private fun TmdbRing(fraction: Double) {
    val pct = (fraction * 100).toInt().coerceIn(0, 100)
    val color = when { pct >= 70 -> TmdbGreen; pct >= 40 -> TmdbYellow; else -> TmdbRed }
    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0xFF081C22))
            val stroke = 2.5.dp.toPx()
            val inset = stroke / 2 + 1.dp.toPx()
            val arc = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(color.copy(alpha = 0.25f), 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            drawArc(color, -90f, 360f * pct / 100f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(pct.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}
