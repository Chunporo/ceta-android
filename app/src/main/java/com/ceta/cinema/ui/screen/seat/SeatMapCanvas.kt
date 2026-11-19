package com.ceta.cinema.ui.screen.seat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ceta.cinema.domain.model.Seat
import com.ceta.cinema.domain.model.SeatKind
import com.ceta.cinema.domain.model.SeatStatus
import com.ceta.cinema.domain.usecase.SeatBlocks
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.CetaPalette
import kotlin.math.max
import kotlin.math.min

/**
 * The seat map as in the design: rows curve gently towards the screen, aisles split the room, a couple seat is one wide
 * seat, the row letter sits on both sides. Pinch to zoom, drag to pan, tap a seat; the chosen seats live in the
 * ViewModel so zooming never loses them. Colour is not the only signal: selected seats show their number on the accent
 * fill, held ones a slash, sold ones a cross, and VIP / couple seats are outlined as well as tinted.
 */
const val MAX_ZOOM = 3f
const val BUTTON_ZOOM = 1.6f
private const val DOUBLE_TAP_MS = 280L

/** One drawn seat: where it sits in map units (before fit/zoom), and which server seat(s) it stands for. */
private class SeatBox(val seat: Seat, val partner: Seat?, val rect: Rect, val curveDrop: Float)

private class MapLayout(val boxes: List<SeatBox>, val rows: List<Pair<Int, Float>>, val width: Float, val height: Float, val label: Float, val seatSide: Float)

private fun layout(seats: List<Seat>, density: Density): MapLayout = with(density) {
    val seatW = 24.dp.toPx(); val gap = 4.dp.toPx(); val aisle = 10.dp.toPx(); val label = 22.dp.toPx()
    val rowGap = 6.dp.toPx(); val sectionGap = 8.dp.toPx()
    val maxRow = seats.maxOfOrNull { it.row } ?: 1
    val maxCol = seats.maxOfOrNull { it.column } ?: 1
    val aisleStarts = SeatBlocks.aisleStarts(maxCol)
    fun aislesBefore(col: Int) = aisleStarts.count { col >= it }
    fun x(col: Int) = label + (col - 1) * (seatW + gap) + aislesBefore(col) * aisle
    val byRow = seats.groupBy { it.row }
    val centre = (maxCol + 1) / 2f
    val curveK = 0.4f * seatW / 21.dp.toPx() // the design drops a seat 0.4px per squared seat distance for a 21px seat
    var y = 0f
    var previousKind: SeatKind? = null
    val rows = ArrayList<Pair<Int, Float>>()
    val boxes = ArrayList<SeatBox>()
    var maxDrop = 0f
    for (row in 1..maxRow) {
        val inRow = byRow[row].orEmpty().sortedBy { it.column }
        if (inRow.isEmpty()) continue
        val kind = inRow.first().kind
        if (previousKind != null && kind != previousKind) y += sectionGap
        previousKind = kind
        rows += row to y
        for (seat in inRow) {
            if (seat.status == SeatStatus.INACTIVE) continue
            val isCouple = seat.kind == SeatKind.COUPLE
            val partner = if (isCouple && seat.column % 2 == 1) inRow.firstOrNull { it.kind == SeatKind.COUPLE && it.column == seat.column + 1 } else null
            // The even half of a couple seat is drawn by its odd half.
            if (isCouple && seat.column % 2 == 0 && inRow.any { it.kind == SeatKind.COUPLE && it.column == seat.column - 1 }) continue
            val w = if (partner != null) 2 * seatW + gap else seatW
            val centreCol = if (partner != null) seat.column + 0.5f else seat.column.toFloat()
            val drop = (centreCol - centre) * (centreCol - centre) * curveK
            maxDrop = max(maxDrop, drop)
            boxes += SeatBox(seat, partner, Rect(x(seat.column), y, x(seat.column) + w, y + seatW), drop)
        }
        y += seatW + rowGap
    }
    MapLayout(boxes, rows, x(maxCol) + seatW + label, y + maxDrop, label, seatW)
}

@Composable
fun SeatMapCanvas(
    seats: List<Seat>, selected: Set<Long>, conflicts: Set<Long>, lone: Set<Long>, onTap: (Long) -> Unit,
    zoom: Float, onZoomChange: (Float) -> Unit, modifier: Modifier = Modifier,
) {
    val c = Ceta.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val map = remember(seats, density) { layout(seats, density) }
    var panX by rememberSaveable { mutableFloatStateOf(0f) }
    var panY by rememberSaveable { mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val fit = min(1f, widthPx / map.width)
        val scale = fit * zoom
        val heightPx = map.height * fit * min(zoom, BUTTON_ZOOM) + with(density) { 8.dp.toPx() }
        // Content narrower than the view is centred; a zoomed-in map is dragged within its own bounds.
        val baseX = max(0f, (widthPx - map.width * scale) / 2f)
        LaunchedEffect(zoom) {
            if (zoom <= 1f) { panX = 0f; panY = 0f }
            else {
                panX = clampX(panX, map.width * scale, widthPx)
                panY = clampY(panY, map.height * scale, heightPx)
            }
        }

        Canvas(
            Modifier.fillMaxWidth().height(with(density) { heightPx.toDp() })
                .pointerInput(widthPx, fit, zoom) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        val newZoom = (zoom * gestureZoom).coerceIn(1f, MAX_ZOOM)
                        val ratio = newZoom / zoom
                        panX = clampX(panX * ratio + (1 - ratio) * (centroid.x - baseX) + pan.x, map.width * fit * newZoom, widthPx)
                        panY = clampY(panY * ratio + (1 - ratio) * centroid.y + pan.y, map.height * fit * newZoom, heightPx)
                        if (newZoom != zoom) onZoomChange(newZoom)
                    }
                }
                .pointerInput(map, zoom, panX, panY, baseX) {
                    // A tap picks a seat at once (no wait to see whether a second tap follows). A second tap at the same spot within
                    // 280 ms is a double-tap: it undoes the seat the first tap toggled and zooms in or out around that point.
                    var lastUp = 0L
                    var lastPos = Offset.Zero
                    fun seatAt(p: Offset): SeatBox? {
                        val x = (p.x - baseX - panX) / scale
                        val y = (p.y - panY) / scale
                        return map.boxes.firstOrNull { x in it.rect.left..it.rect.right && y in (it.rect.top + it.curveDrop)..(it.rect.bottom + it.curveDrop) }
                    }
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                        val doubled = up.uptimeMillis - lastUp <= DOUBLE_TAP_MS && (up.position - lastPos).getDistance() < 48.dp.toPx()
                        if (doubled) {
                            lastUp = 0L
                            seatAt(up.position)?.let { onTap(it.seat.id) }
                            val newZoom = if (zoom > 1.01f) 1f else BUTTON_ZOOM
                            val ratio = newZoom / zoom
                            panX = clampX(panX * ratio + (1 - ratio) * (up.position.x - baseX), map.width * fit * newZoom, widthPx)
                            panY = clampY(panY * ratio + (1 - ratio) * up.position.y, map.height * fit * newZoom, heightPx)
                            onZoomChange(newZoom)
                        } else {
                            lastUp = up.uptimeMillis; lastPos = up.position
                            seatAt(up.position)?.let { onTap(it.seat.id) }
                        }
                    }
                },
        ) {
            val showNumbers = zoom > 1.2f
            withTransform({
                translate(left = baseX + panX, top = panY)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                val labelStyle = TextStyle(color = c.mu, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                map.rows.forEach { (row, y) ->
                    val laid = measurer.measure(('A' + row - 1).toString(), labelStyle)
                    val cy = y + map.seatSide / 2f - laid.size.height / 2f
                    drawText(laid, topLeft = Offset((map.label - laid.size.width) / 2f, cy))
                    drawText(laid, topLeft = Offset(map.width - map.label + (map.label - laid.size.width) / 2f, cy))
                }
                map.boxes.forEach { box ->
                    val isSelected = box.seat.id in selected || (box.partner != null && box.partner.id in selected)
                    val isConflict = box.seat.id in conflicts || (box.partner != null && box.partner.id in conflicts)
                    drawSeat(box, isSelected, isConflict, box.seat.id in lone, showNumbers, c, measurer)
                }
            }
        }
    }
}

private fun clampX(x: Float, contentWidth: Float, width: Float) =
    if (contentWidth <= width) 0f else x.coerceIn(width - contentWidth, 0f)

private fun clampY(y: Float, contentHeight: Float, height: Float) =
    if (contentHeight <= height) 0f else y.coerceIn(height - contentHeight, 0f)

private fun DrawScope.drawSeat(box: SeatBox, selected: Boolean, conflict: Boolean, lone: Boolean, showNumber: Boolean, c: CetaPalette, measurer: TextMeasurer) {
    val seat = box.seat
    val topLeft = Offset(box.rect.left, box.rect.top + box.curveDrop)
    val size = Size(box.rect.width, box.rect.height)
    val top = CornerRadius(8.dp.toPx()); val bottom = CornerRadius(5.dp.toPx())
    val shape = Path().apply { addRoundRect(RoundRect(Rect(topLeft, size), top, top, bottom, bottom)) }
    val stroke = 1.5.dp.toPx()
    val dashed = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())))
    val tint = when (seat.kind) { SeatKind.VIP -> c.ac2; SeatKind.COUPLE -> c.ac3; SeatKind.STANDARD -> c.ln2 }
    val gone = seat.status == SeatStatus.SOLD || seat.status == SeatStatus.HELD
    val number = if (box.partner != null) seat.label + "·" + box.partner.label.dropWhile { !it.isDigit() } else seat.label.dropWhile { !it.isDigit() }
    val (text, textColor) = when {
        selected -> number to Color.White
        lone -> number to c.ac
        seat.status == SeatStatus.SOLD -> "×" to c.mu
        showNumber && !gone -> number to c.mu
        else -> "" to c.mu
    }
    when {
        selected -> {
            // A soft ring around a chosen seat (box-shadow 0 0 0 3px accent-soft in the design).
            val ring = 3.dp.toPx()
            val outer = Path().apply { addRoundRect(RoundRect(Rect(topLeft - Offset(ring, ring), Size(size.width + 2 * ring, size.height + 2 * ring)), CornerRadius(top.x + ring), CornerRadius(top.x + ring), CornerRadius(bottom.x + ring), CornerRadius(bottom.x + ring))) }
            drawPath(outer, c.acs)
            drawPath(shape, c.ac); drawPath(shape, c.ac, style = Stroke(stroke))
        }
        // A seat that would be left alone: soft accent fill and outline (the number is drawn in accent below).
        lone -> { drawPath(shape, c.acs); drawPath(shape, c.ac, style = Stroke(stroke)) }
        gone -> drawPath(shape, c.ln2, style = dashed)
        else -> {
            drawPath(shape, if (seat.kind == SeatKind.STANDARD) c.sf2 else tint.copy(alpha = 0.22f))
            drawPath(shape, tint, style = Stroke(stroke))
        }
    }
    if (seat.status == SeatStatus.HELD && !selected) {
        drawLine(c.mu, topLeft + Offset(size.width * 0.2f, size.height * 0.8f), topLeft + Offset(size.width * 0.8f, size.height * 0.2f), stroke)
    }
    if (text.isNotEmpty()) {
        val laid = measurer.measure(text, TextStyle(color = textColor, fontSize = if (text == "×") 12.sp else 9.sp, fontWeight = FontWeight.Bold))
        drawText(laid, topLeft = topLeft + Offset((size.width - laid.size.width) / 2f, (size.height - laid.size.height) / 2f))
    }
    if (conflict) drawPath(shape, Color(0xFFE5484D), style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
}
