package com.ceta.cinema.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.ceta.cinema.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.ui.theme.Ceta

/**
 * The seat-hold countdown (design item 3), shown on seats, snacks and payment: a soft accent pill with MM:SS, turning solid accent
 * with white text for the last two minutes. It never shows negative time.
 */
@Composable
fun HoldChip(secondsLeft: Long, warning: Boolean, modifier: Modifier = Modifier) {
    val c = Ceta.colors
    val time = Formatters.clock(secondsLeft)
    val bg by animateColorAsState(if (warning) c.ac else c.acs, tween(300), label = "holdBg")
    val fg by animateColorAsState(if (warning) Color.White else c.ac, tween(300), label = "holdFg")
    Row(
        modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(bg).padding(horizontal = 12.dp).semantics { contentDescription = time },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Timer, null, tint = fg, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(5.dp))
        Text(time, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, style = TextStyle(fontFeatureSettings = "tnum"))
    }
}

/** A once-a-second tick that only runs while the screen is on show. */
@Composable
fun EverySecond(onTick: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); onTick() } }
}

/** Remembers which holds already showed the "two minutes left" toast, so one hold warns once however many screens it passes through. */
object HoldWarnings {
    private val shown = HashSet<String>()
    @Synchronized fun firstTime(key: String): Boolean = shown.add(key)
}

/**
 * The two hold notices of design item 3 for a screen that follows a booking's hold: a toast once when two minutes are left, and
 * [onExpired] when the time is up (the caller sends the customer back to the seat map, which clears the seats and says so).
 */
@Composable
fun HoldToasts(key: String, warning: Boolean, expired: Boolean, snackbar: SnackbarHostState, onExpired: () -> Unit) {
    val text = stringResource(R.string.hold_warn_toast)
    LaunchedEffect(warning, expired) { if (warning && !expired && HoldWarnings.firstTime(key)) snackbar.showSnackbar(text) }
    LaunchedEffect(expired) { if (expired) onExpired() }
}
