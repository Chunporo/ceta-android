package com.ceta.cinema.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface

/**
 * The status icon of the batch-3 screens (payment failed, hold expired, no results): a soft accent ring around a surface disc
 * that carries the icon. 112 / 80 / 44 on the big screens, smaller for an empty state.
 */
@Composable
fun RingIcon(icon: ImageVector, modifier: Modifier = Modifier, outer: Dp = 112.dp, inner: Dp = 80.dp, glyph: Dp = 44.dp) {
    val c = Ceta.colors
    Box(modifier.size(outer).clip(CircleShape).background(c.acs), contentAlignment = Alignment.Center) {
        Box(Modifier.size(inner).cardSurface(CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.ac, modifier = Modifier.size(glyph))
        }
    }
}
