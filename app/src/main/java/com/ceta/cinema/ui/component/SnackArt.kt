package com.ceta.cinema.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.ceta.cinema.ui.theme.Ceta

private enum class Piece(val w: Float, val h: Float) { POP(44f, 59f), CUP(24f, 48f), SNACK(36f, 30f) }

/** What is drawn for one product: pieces with a scale, and whether the popcorn is caramel. [glow] is the colour behind them. */
private class Art(val pieces: List<Triple<Piece, Float, Boolean>>, val glow: Color)

/** Vector illustrations for the five snack products (CETA Snack Assets), used until real product photos exist. */
private fun artFor(code: String?, name: String): Art {
    val key = (code ?: name).lowercase()
    return when {
        "solo" in key -> Art(listOf(Triple(Piece.POP, .85f, false), Triple(Piece.CUP, 1f, false)), Color(0xFFD9A25B))
        "couple" in key || "doi" in key || "đôi" in key -> Art(listOf(Triple(Piece.POP, 1f, false), Triple(Piece.CUP, 1f, false), Triple(Piece.CUP, 1f, false)), Color(0xFFE08A6E))
        "family" in key || "gia" in key -> Art(listOf(Triple(Piece.POP, .72f, false), Triple(Piece.POP, .72f, false), Triple(Piece.CUP, .75f, false), Triple(Piece.SNACK, .75f, false)), Color(0xFFB58BE0))
        "caramel" in key -> Art(listOf(Triple(Piece.POP, 1.05f, true)), Color(0xFFCDB24E))
        "drink" in key || "nuoc" in key || "nước" in key -> Art(listOf(Triple(Piece.CUP, 1.15f, false)), Color(0xFF6FB0E0))
        else -> Art(listOf(Triple(Piece.POP, 1f, false), Triple(Piece.CUP, 1f, false)), Color(0xFFD9A25B))
    }
}

private fun svg(d: String): Path = PathParser().parsePathString(d).toPath()

/**
 * A snack product drawn in vectors on the soft glow of its colour (design item 12). The cell is meant for 84x84dp with round corners,
 * but it scales to whatever size it is given.
 */
@Composable
fun SnackArt(code: String?, name: String, modifier: Modifier = Modifier) {
    val c = Ceta.colors
    val art = remember(code, name) { artFor(code, name) }
    val paths = remember {
        mapOf(
            "bucket" to svg("M8 30H52L46 78H14Z"), "stripes" to svg("M8 30H15.3L19.3 78H14Z M22.7 30H30V78H24.7Z M37.3 30H44.7L40.7 78H35.3Z"),
            "cupBody" to svg("M5 23H35L31 78H9Z"), "cupLabel" to svg("M6.3 39H33.7L32.6 54H7.4Z"), "cupC" to svg("M23.5 43A5 5 0 1 0 23.5 50"),
            "t1" to svg("M9 26L19 5L28 26Z"), "t2" to svg("M23 26L35 2L44 26Z"), "t3" to svg("M38 26L48 9L54 26Z"), "tray" to svg("M3 24H57L51 48H9Z"),
        )
    }
    Box(modifier.background(c.sf2)) {
        Canvas(Modifier.fillMaxSize()) {
            // The glow sits low in the cell, like the design's "circle at 50% 78%".
            drawRect(Brush.radialGradient(listOf(art.glow.copy(alpha = 0.5f), Color.Transparent), Offset(size.width * 0.5f, size.height * 0.78f), size.width * 0.7f))
            val k = size.width / 84f * 0.966f                       // the design draws at k = 2.3 in a 200-wide cell
            val widths = art.pieces.map { it.first.w * it.second * k }
            val overlap = 10f * k
            val total = widths.sum() - overlap * (art.pieces.size - 1)
            var x = (size.width - total) / 2f
            val bottom = size.height - size.height * 22f / 200f
            art.pieces.forEachIndexed { i, (piece, scale, caramel) ->
                val w = piece.w * scale * k
                val h = piece.h * scale * k
                withTransform({ translate(x, bottom - h) }) { drawPiece(piece, w, h, caramel, paths, c.ac) }
                x += w - overlap
            }
        }
    }
}

private fun DrawScope.drawPiece(piece: Piece, w: Float, h: Float, caramel: Boolean, paths: Map<String, Path>, accent: Color) {
    when (piece) {
        Piece.POP -> withTransform({ scale(w / 60f, h / 80f, Offset.Zero) }) {
            val fill = if (caramel) Color(0xFFF2B755) else Color(0xFFFFF3D1)
            val line = if (caramel) Color(0xFFC98322) else Color(0xFFE6C36E)
            listOf(15f to 22f to 9f, 30f to 15f to 10f, 45f to 22f to 9f, 22f to 27f to 8f, 38f to 27f to 8f).forEach { (pos, r) ->
                drawCircle(fill, r, Offset(pos.first, pos.second)); drawCircle(line, r, Offset(pos.first, pos.second), style = Stroke(1.2f))
            }
            drawPath(paths.getValue("bucket"), Color.White)
            drawPath(paths.getValue("stripes"), accent)
            drawRoundRect(accent, Offset(6f, 27f), Size(48f, 6f), CornerRadius(3f))
        }
        Piece.CUP -> withTransform({ scale(w / 40f, h / 80f, Offset.Zero) }) {
            rotate(10f, Offset(24f, 10f)) { drawRoundRect(Color(0xFFF4F3F1), Offset(22f, 1f), Size(4f, 18f), CornerRadius(1.5f)) }
            drawRoundRect(Color(0xFFE4E2DE), Offset(3f, 15f), Size(34f, 8f), CornerRadius(4f))
            drawPath(paths.getValue("cupBody"), accent)
            drawPath(paths.getValue("cupLabel"), Color.White)
            drawPath(paths.getValue("cupC"), accent, style = Stroke(2.6f, cap = StrokeCap.Round))
        }
        Piece.SNACK -> withTransform({ scale(w / 60f, h / 50f, Offset.Zero) }) {
            listOf("t1", "t2", "t3").forEach { k ->
                drawPath(paths.getValue(k), Color(0xFFF3C14B)); drawPath(paths.getValue(k), Color(0xFFD49A22), style = Stroke(1.2f, join = StrokeJoin.Round))
            }
            drawPath(paths.getValue("tray"), Color(0xFF9A3A1F))
            drawLine(Color.White.copy(alpha = 0.5f), Offset(5f, 30f), Offset(55f, 30f), strokeWidth = 3f)
        }
    }
}
