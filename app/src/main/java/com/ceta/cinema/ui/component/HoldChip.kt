package com.ceta.cinema.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.Warn

/** The seat-hold countdown: neutral, then yellow for the last two minutes. It never shows negative time. */
@Composable
fun HoldChip(secondsLeft: Long, warning: Boolean, modifier: Modifier = Modifier) {
    val c = Ceta.colors
    val time = Formatters.mmss(secondsLeft)
    Row(
        modifier.clip(RoundedCornerShape(20.dp)).background(if (warning) Warn.copy(alpha = 0.25f) else c.sf).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Timer, null, tint = if (warning) Warn else c.mu, modifier = Modifier.padding(end = 6.dp))
        Text(stringResource(if (warning) R.string.hold_warning else R.string.hold_left, time), style = MaterialTheme.typography.labelMedium, color = if (warning) c.tx else c.mu)
    }
}

/** A once-a-second tick that only runs while the screen is on show. */
@Composable
fun EverySecond(onTick: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); onTick() } }
}
