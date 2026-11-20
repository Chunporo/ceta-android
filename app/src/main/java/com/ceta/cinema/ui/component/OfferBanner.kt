package com.ceta.cinema.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import com.ceta.cinema.domain.model.Promotion

/** The hue of an offer, taken from its code so every offer keeps its own colour. */
fun offerHue(promo: Promotion): Float = (promo.code.hashCode().toUInt().toLong() % 360).toFloat()

/**
 * The offer banner of design item 14: the offer's picture (or, with none, a gradient of its hue) under a dark overlay of that hue,
 * and the big highlight (50K, -20%, 49K, IMAX) on the right. [content] is drawn on top, on the left: keep the title to about 55 % of
 * the width. Production should use banners marketing designs; this is the generated fallback.
 */
@Composable
fun OfferBanner(promo: Promotion, modifier: Modifier = Modifier, highlightSize: TextUnit, content: @Composable BoxScope.() -> Unit) {
    val hue = offerHue(promo)
    val dark = Color.hsv(hue, 0.72f, 0.27f)
    Box(modifier) {
        if (promo.imageUrl.isNullOrBlank()) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color.hsv(hue, 0.6f, 0.62f), Color.hsv((hue + 40) % 360, 0.65f, 0.45f)))))
        } else AsyncImage(promo.imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        // 100deg overlay: nearly solid under the title, lighter in the middle, a little darker again under the highlight.
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(Brush.horizontalGradient(0f to dark.copy(alpha = 0.94f), 0.3f to dark.copy(alpha = 0.94f), 0.75f to dark.copy(alpha = 0.45f), 1f to dark.copy(alpha = 0.7f)))
            },
        )
        promo.highlight?.let {
            Text(
                it, color = Color.White, fontSize = highlightSize, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.04f * highlightSize.value).sp,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
            )
        }
        content()
    }
}
