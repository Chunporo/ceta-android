package com.ceta.cinema.ui.screen.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ceta.cinema.R
import com.ceta.cinema.ui.component.CetaLogo
import com.ceta.cinema.ui.theme.Ceta

private val Panel = Color(0xFF202027)   // the dark UI card of the key art

/**
 * The key art of an onboarding page (design item 13): the panel's background and the small illustration built from real UI pieces.
 * The design lays these over a photo of a cinema; there is no licensed photo, so the panel is a dark gradient with the page's glow.
 */
@Composable
fun OnboardingArt(page: Int, modifier: Modifier = Modifier) {
    val c = Ceta.colors
    val glow = when (page) { 0 -> c.ac; 1 -> c.ac2; else -> Color(0xFFE2A53A) }  // hues 32, 295, 60
    Box(
        modifier.drawBehind {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF241A1C), Color(0xFF101014))))
            drawRect(Brush.verticalGradient(listOf(Color(0xFF101014).copy(alpha = 0.62f), Color(0xFF101014).copy(alpha = 0.88f))))
            drawRect(Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent), Offset(size.width, 0f), size.width * 0.9f))
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.padding(bottom = 36.dp)) {
            when (page) { 0 -> BookingArt(); 1 -> SeatMapArt(); else -> MemberArt() }
        }
    }
}

/** Page 1: a showtime card, a row of seats and a "Paid" pill, tilted a little. */
@Composable
private fun BookingArt() {
    val c = Ceta.colors
    Column(Modifier.rotate(-4f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.width(260.dp).clip(RoundedCornerShape(22.dp)).background(Panel).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(width = 48.dp, height = 70.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(c.ac, c.ac.copy(alpha = 0.6f)))))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                Box(Modifier.width(140.dp).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f)))
                Box(Modifier.width(96.dp).height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f)))
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("19:45", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(c.ac).padding(horizontal = 10.dp, vertical = 5.dp))
                    Text("21:50", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(10.dp)).border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
        Row(Modifier.width(236.dp).clip(RoundedCornerShape(20.dp)).background(Panel).padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) { Box(Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(if (it == 2 || it == 3) c.ac else Color.White.copy(alpha = 0.2f))) }
        }
        Row(
            Modifier.rotate(8f).clip(RoundedCornerShape(28.dp)).background(c.ac).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Text(stringResource(R.string.ob_art_paid), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** Page 2: a curved 6 x 9 seat map under a glowing curved screen: two VIP rows, some sold seats, two chosen ones. */
@Composable
private fun SeatMapArt() {
    val c = Ceta.colors
    val seat = 20.dp
    val gap = 6.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Canvas(Modifier.width(220.dp).height(34.dp)) {
            val stroke = 3.dp.toPx()
            drawArc(c.ac.copy(alpha = 0.25f), 190f, 160f, false, Offset(0f, stroke), Size(size.width, size.height * 2f), style = Stroke(stroke * 4f))
            drawArc(c.ac, 190f, 160f, false, Offset(0f, stroke), Size(size.width, size.height * 2f), style = Stroke(stroke))
        }
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            repeat(6) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(9) { col ->
                        // The rows curve: the middle seats sit higher than the outer ones, like the real map.
                        val drop = (0.5f * (col + 1 - 5) * (col + 1 - 5)).dp
                        val vip = row >= 3 && row <= 4
                        val sold = (row to col) in setOf(0 to 3, 1 to 5, 2 to 1, 2 to 7, 4 to 4, 5 to 2)
                        val chosen = (row to col) in setOf(2 to 4, 2 to 5)
                        val base = Modifier.offset(y = drop).size(seat).clip(RoundedCornerShape(6.dp))
                        when {
                            chosen -> Box(base.shadow(8.dp, RoundedCornerShape(6.dp), ambientColor = c.ac, spotColor = c.ac).background(c.ac))
                            sold -> Box(base.drawBehind {
                                drawRoundRect(Color.White.copy(alpha = 0.3f), style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))), cornerRadius = CornerRadius(6.dp.toPx()))
                            })
                            vip -> Box(base.background(c.ac2.copy(alpha = 0.85f)))
                            else -> Box(base.background(Color.White.copy(alpha = 0.2f)))
                        }
                    }
                }
            }
        }
    }
}

/** Page 3: a tilted membership card over a second one, with a "+120 points" chip. */
@Composable
private fun MemberArt() {
    val c = Ceta.colors
    Box(Modifier.size(width = 280.dp, height = 190.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 240.dp, height = 150.dp).offset(x = 12.dp, y = 8.dp).rotate(7f).clip(RoundedCornerShape(22.dp)).background(c.ac2))
        Box(Modifier.size(width = 240.dp, height = 150.dp).rotate(-6f).clip(RoundedCornerShape(22.dp)).background(c.ac)) {
            Box(Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-40).dp).size(150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)))
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CetaLogo(28.dp, tile = Color.White, mark = c.ac)
                Spacer(Modifier.weight(1f))
                Text("VIP", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text("7.450", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, style = TextStyle(fontFeatureSettings = "tnum"))
                Text(stringResource(R.string.ob_art_points), color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }
        Text("+120", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.BottomEnd).clip(RoundedCornerShape(14.dp)).background(Panel).padding(horizontal = 12.dp, vertical = 8.dp))
    }
}
