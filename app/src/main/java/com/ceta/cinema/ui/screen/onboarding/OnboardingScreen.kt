package com.ceta.cinema.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EventSeat
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ceta.cinema.R
import com.ceta.cinema.ui.component.Dot
import com.ceta.cinema.ui.component.PrimaryButton
import com.ceta.cinema.ui.theme.Ceta

private val pages = listOf(
    Triple(R.string.ob_title_1, R.string.ob_body_1, Icons.Rounded.Bolt),
    Triple(R.string.ob_title_2, R.string.ob_body_2, Icons.Rounded.EventSeat),
    Triple(R.string.ob_title_3, R.string.ob_body_3, Icons.Rounded.Redeem),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val c = Ceta.colors
    var index by rememberSaveable { mutableIntStateOf(0) }
    val (title, body, icon) = pages[index]
    val last = index == pages.lastIndex
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text(stringResource(R.string.ds_skip), color = c.mu) }
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(c.acs, c.ac2.copy(alpha = 0.25f), c.ac3.copy(alpha = 0.2f)))),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = c.ac, modifier = Modifier.size(96.dp)) }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, color = c.tx, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = c.mu, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            pages.indices.forEach { Dot(active = it == index) }
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(stringResource(if (last) R.string.onboarding_start else R.string.onboarding_next), onClick = { if (last) onFinish() else index++ })
    }
}
