package com.ceta.cinema.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ceta.cinema.ui.findActivity

val LocalCeta = staticCompositionLocalOf { DarkPalette }

/** Read the design tokens anywhere: {@code Ceta.colors.ac}. */
object Ceta {
    val colors: CetaPalette
        @Composable @ReadOnlyComposable get() = LocalCeta.current
}

@Composable
fun CetaTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = p.ac, onPrimary = androidx.compose.ui.graphics.Color.White,
        background = p.bg, onBackground = p.tx, surface = p.sf, onSurface = p.tx,
        surfaceVariant = p.sf2, onSurfaceVariant = p.mu, outline = p.ln2, outlineVariant = p.ln,
        error = p.ac, secondary = p.ac2, tertiary = p.ac3,
    )
    // Dark icons on the light theme and light icons on the dark one; the system default does not follow our own switch.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }
    CompositionLocalProvider(LocalCeta provides p) {
        MaterialTheme(colorScheme = scheme, typography = CetaTypography, content = content)
    }
}
