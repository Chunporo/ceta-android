package com.ceta.cinema.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ceta.cinema.R

/** Be Vietnam Pro, bundled (OFL): it carries every Vietnamese diacritic the design uses. */
val BeVietnamPro = FontFamily(
    Font(R.font.bevietnampro_regular, FontWeight.Normal),
    Font(R.font.bevietnampro_medium, FontWeight.Medium),
    Font(R.font.bevietnampro_semibold, FontWeight.SemiBold),
    Font(R.font.bevietnampro_bold, FontWeight.Bold),
)

private fun style(size: Int, line: Int, weight: FontWeight) =
    TextStyle(fontFamily = BeVietnamPro, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

val CetaTypography = Typography(
    headlineMedium = style(28, 34, FontWeight.Bold),
    titleLarge = style(22, 28, FontWeight.Bold),
    titleMedium = style(18, 24, FontWeight.SemiBold),
    titleSmall = style(15, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(13, 18, FontWeight.Normal),
    labelLarge = style(15, 20, FontWeight.SemiBold),
    labelMedium = style(13, 16, FontWeight.Medium),
    labelSmall = style(12, 16, FontWeight.Medium),
)
