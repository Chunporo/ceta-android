package com.ceta.cinema.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** The design's tokens (CETA App.dc.html): one palette per mode, accent is a warm red-orange. */
@Immutable
class CetaPalette(
    val dark: Boolean,
    val bg: Color,        // screen background
    val sf: Color,        // cards
    val sf2: Color,       // raised / selected-neutral
    val tx: Color,        // text
    val mu: Color,        // muted text
    val ln: Color,        // hairline
    val ln2: Color,       // stronger line / disabled
    val ac: Color,        // primary accent
    val ac2: Color,       // violet
    val ac3: Color,       // pink
    val acs: Color,       // accent, soft fill
)

val DarkPalette = CetaPalette(
    dark = true,
    bg = Color(0xFF0F0F12), sf = Color(0xFF1A1A1F), sf2 = Color(0xFF26262D),
    tx = Color(0xFFF4F3F1), mu = Color(0xFF9D9DA7),
    ln = Color(0x12FFFFFF), ln2 = Color(0x29FFFFFF),
    ac = Color(0xFFEB5A40), ac2 = Color(0xFF8E6AE6), ac3 = Color(0xFFE5527E), acs = Color(0x29EB5A40),
)

val LightPalette = CetaPalette(
    dark = false,
    bg = Color(0xFFF6F5F3), sf = Color(0xFFFFFFFF), sf2 = Color(0xFFEEEDEA),
    tx = Color(0xFF17171B), mu = Color(0xFF66666F),
    ln = Color(0x12000000), ln2 = Color(0x29000000),
    ac = Color(0xFFD9503C), ac2 = Color(0xFF7A52D6), ac3 = Color(0xFFD4466F), acs = Color(0x1FD9503C),
)

/** Age-rating badge colours (P, K, T13, T16, T18). */
fun ageColor(age: String?): Color = when (age) {
    "P" -> Color(0xFF3FA66B)
    "K" -> Color(0xFF3B95B5)
    "T13" -> Color(0xFFC79A2E)
    "T16" -> Color(0xFFD8742F)
    "T18" -> Color(0xFFCF3B33)
    else -> Color(0xFF8A8A92)
}

val Warn = Color(0xFFF2A93B)
