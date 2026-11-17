package com.ceta.cinema.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val Scheme = lightColorScheme(
    primary = CetaColors.Accent,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = CetaColors.AccentDark,
    background = CetaColors.Surface,
    surface = CetaColors.Surface,
    onSurface = CetaColors.OnSurface,
    error = CetaColors.Danger,
)

@Composable
fun CetaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = CetaTypography, content = content)
}
