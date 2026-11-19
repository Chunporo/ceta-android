package com.ceta.cinema.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val Ink04 = Color(0x0A17171B)
private val Ink06 = Color(0x0F17171B)

/**
 * A surface card with the design's `--sh` token: in light mode a faint two-layer shadow (0 1px 2px at 4% and 0 6px 18px at 6% of the
 * ink colour), in dark mode none (the dark surfaces already separate from the background). Use it for surface cards rounded 16-28dp.
 */
@Composable
fun Modifier.cardSurface(shape: Shape, color: Color = Ceta.colors.sf): Modifier {
    val lifted = if (Ceta.colors.dark) this else this
        .shadow(1.dp, shape, clip = false, ambientColor = Ink04, spotColor = Ink04)
        .shadow(6.dp, shape, clip = false, ambientColor = Ink06, spotColor = Ink06)
    return lifted.clip(shape).background(color)
}
