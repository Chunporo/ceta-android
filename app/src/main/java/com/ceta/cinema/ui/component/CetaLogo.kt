package com.ceta.cinema.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ceta.cinema.ui.theme.Ceta

private val SMALL_LOGO = 40.dp

/**
 * The CETA mark, direction 1a "Ticket stub" (CETA Logo.dc.html): a white C on a rounded tile with a notch cut out of each
 * side and a dotted perforation line. Drawn on a 120-unit grid like the design, so it scales to any size.
 */
@Composable
fun CetaLogo(size: Dp, modifier: Modifier = Modifier, tile: Color = Ceta.colors.ac, mark: Color = Color.White) {
    // Small sizes (40dp and below) get a heavier C and no perforation line, which would only turn to noise.
    val small = size <= SMALL_LOGO
    Canvas(modifier.size(size).semantics { contentDescription = "CETA" }) {
        val u = this.size.width / 120f
        val shape = Path.combine(
            PathOperation.Difference,
            Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, Size(120f * u, 120f * u)), CornerRadius(32f * u))) },
            Path().apply {
                addOval(Rect(Offset(-11f * u, 49f * u), Size(22f * u, 22f * u)))
                addOval(Rect(Offset(109f * u, 49f * u), Size(22f * u, 22f * u)))
            },
        )
        drawPath(shape, tile)
        // The C: radius 24 around the centre, open to the right between -51.2 and +51.2 degrees.
        drawArc(mark, startAngle = -51.2f, sweepAngle = -257.6f, useCenter = false, topLeft = Offset(36f * u, 36f * u),
            size = Size(48f * u, 48f * u), style = Stroke(width = (if (small) 17f else 15f) * u, cap = StrokeCap.Round))
        if (!small) for (i in 0..8) drawCircle(mark, radius = 1.75f * u, center = Offset(94f * u, (28f + 8f * i) * u))
    }
}

/** The logo with the "CETA" wordmark (800 weight, letter-spacing -0.03em) beside it, as in the onboarding, home and ticket headers. */
@Composable
fun CetaWordmark(logoSize: Dp, textSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier,
                 tile: Color = Ceta.colors.ac, mark: Color = Color.White, textColor: Color = Ceta.colors.tx) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        CetaLogo(logoSize, tile = tile, mark = mark)
        Spacer(Modifier.width((logoSize.value * 0.28f).dp))
        Text("CETA", color = textColor, fontSize = textSize, fontWeight = FontWeight.ExtraBold, letterSpacing = (textSize.value * -0.03f).sp)
    }
}
