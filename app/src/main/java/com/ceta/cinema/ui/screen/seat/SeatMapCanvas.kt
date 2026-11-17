package com.ceta.cinema.ui.screen.seat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ceta.cinema.domain.model.Seat
import com.ceta.cinema.domain.model.SeatKind
import com.ceta.cinema.domain.model.SeatStatus
import com.ceta.cinema.ui.theme.Ceta
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * The seat map: pinch to zoom, drag to pan, tap a seat. Drawn on a Canvas rather than a grid because it must zoom and
 * because rooms are not rectangles of identical cells. The chosen seats live in the ViewModel, so zooming never loses them.
 * Colour is never the only signal: selected seats carry a tick, held ones a slash, sold ones a cross.
 */
@Composable
fun SeatMapCanvas(seats: List<Seat>, selected: Set<Long>, conflicts: Set<Long>, onTap: (Long) -> Unit, modifier: Modifier = Modifier) {
    val c = Ceta.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val maxRow = seats.maxOfOrNull { it.row } ?: 1
    val maxCol = seats.maxOfOrNull { it.column } ?: 1
    val cell = with(density) { 36.dp.toPx() }
    val gap = with(density) { 4.dp.toPx() }
    val label = with(density) { 22.dp.toPx() }
    val contentW = label + maxCol * cell
    val contentH = maxRow * cell
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }
    val byCell = remember(seats) { seats.associateBy { it.row to it.column } }
    val colors = SeatColors(c.ln2, c.sf2, c.ac, c.ac2, c.ac3, c.mu, c.tx)

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val fit = min(1f, widthPx / contentW)
        val scale = fit * zoom
        // Content smaller than the view is centred; a zoomed-in map can be dragged within its own bounds.
        val baseX = max(0f, (widthPx - contentW * scale) / 2f)
        val heightPx = contentH * fit + with(density) { 8.dp.toPx() }

        Canvas(
            Modifier.fillMaxWidth().height(with(density) { heightPx.toDp() })
                .pointerInput(widthPx, fit) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        val newZoom = (zoom * gestureZoom).coerceIn(1f, 3f)
                        val ratio = newZoom / zoom
                        // keep the point under the fingers where it is while zooming
                        panX = clampXFor(panX * ratio + (1 - ratio) * (centroid.x - baseX) + pan.x, contentW * fit * newZoom, widthPx, baseXFor(widthPx, contentW * fit * newZoom))
                        panY = clampYFor(panY * ratio + (1 - ratio) * centroid.y + pan.y, contentH * fit * newZoom, heightPx)
                        zoom = newZoom
                    }
                }
                .pointerInput(seats, zoom, panX, panY) {
                    detectTapGestures { p ->
                        val x = (p.x - baseX - panX) / scale - label
                        val y = (p.y - panY) / scale
                        if (x >= 0 && y >= 0) {
                            byCell[(floor(y / cell).toInt() + 1) to (floor(x / cell).toInt() + 1)]?.let { onTap(it.id) }
                        }
                    }
                },
        ) {
            withTransform({
                translate(left = baseX + panX, top = panY)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                for (row in 1..maxRow) {
                    drawText(measurer, ('A' + row - 1).toString(), Offset(2f, (row - 1) * cell + cell / 2 - 9f), style = TextStyle(color = colors.muted, fontSize = 11.sp))
                }
                seats.forEach { seat ->
                    val topLeft = Offset(label + (seat.column - 1) * cell + gap / 2, (seat.row - 1) * cell + gap / 2)
                    drawSeat(seat, topLeft, cell - gap, seat.id in selected, seat.id in conflicts, colors, measurer)
                }
            }
        }
    }
}

private fun baseXFor(width: Float, contentWidth: Float) = max(0f, (width - contentWidth) / 2f)
private fun clampXFor(x: Float, contentWidth: Float, width: Float, base: Float) =
    if (contentWidth <= width) 0f else x.coerceIn(width - contentWidth - base, 0f - base)
private fun clampYFor(y: Float, contentHeight: Float, height: Float) = if (contentHeight <= height) 0f else y.coerceIn(height - contentHeight, 0f)

private class SeatColors(val line: Color, val free: Color, val accent: Color, val vip: Color, val couple: Color, val muted: Color, val text: Color)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSeat(
    seat: Seat, topLeft: Offset, side: Float, selected: Boolean, conflict: Boolean, colors: SeatColors, measurer: androidx.compose.ui.text.TextMeasurer,
) {
    if (seat.status == SeatStatus.INACTIVE) return
    val size = Size(side, side)
    val radius = CornerRadius(side * 0.28f)
    val tint = when (seat.kind) { SeatKind.VIP -> colors.vip; SeatKind.COUPLE -> colors.couple; SeatKind.STANDARD -> colors.line }
    when {
        selected -> {
            drawRoundRect(colors.accent, topLeft, size, radius)
            drawText(measurer, "✓", topLeft + Offset(side * 0.26f, side * 0.12f), style = TextStyle(color = Color.White, fontSize = 15.sp))
        }
        seat.status == SeatStatus.SOLD -> {
            drawRoundRect(colors.muted.copy(alpha = 0.55f), topLeft, size, radius)
            val inset = side * 0.3f
            drawLine(colors.text.copy(alpha = 0.7f), topLeft + Offset(inset, inset), topLeft + Offset(side - inset, side - inset), 2f)
            drawLine(colors.text.copy(alpha = 0.7f), topLeft + Offset(side - inset, inset), topLeft + Offset(inset, side - inset), 2f)
        }
        seat.status == SeatStatus.HELD || seat.status == SeatStatus.HELD_BY_ME -> {
            drawRoundRect(colors.muted.copy(alpha = 0.35f), topLeft, size, radius)
            drawLine(colors.text.copy(alpha = 0.6f), topLeft + Offset(side * 0.2f, side * 0.8f), topLeft + Offset(side * 0.8f, side * 0.2f), 1.5f)
        }
        else -> {
            drawRoundRect(if (seat.kind == SeatKind.STANDARD) colors.free else tint.copy(alpha = 0.22f), topLeft, size, radius)
            drawRoundRect(tint, topLeft, size, radius, style = Stroke(1.5f))
        }
    }
    if (conflict) drawRoundRect(Color(0xFFE5484D), topLeft, size, radius, style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
}
