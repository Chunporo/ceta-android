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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import com.ceta.cinema.R
import com.ceta.cinema.ui.component.CetaWordmark
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
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            CetaWordmark(30.dp, 22.sp)
            Box(Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(c.sf2).clickable(onClick = onFinish).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.ds_skip), color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        // The key-art panel: a soft gradient with the page's icon in a frosted tile at the top left, as in the design.
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(c.ac.copy(alpha = 0.55f), c.ac2.copy(alpha = 0.35f), c.ac3.copy(alpha = 0.3f)))),
        ) {
            Box(Modifier.padding(22.dp).size(84.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(title), color = c.tx, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 33.sp, letterSpacing = (-0.56).sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = c.mu, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) { pages.indices.forEach { Dot(active = it == index) } }
            Row(
                Modifier.height(56.dp).clip(RoundedCornerShape(28.dp)).background(c.ac).clickable { if (last) onFinish() else index++ }.padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(if (last) R.string.onboarding_start else R.string.onboarding_next), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}
