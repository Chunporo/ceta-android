package com.ceta.cinema.domain.usecase

import com.ceta.cinema.domain.model.Seat
import com.ceta.cinema.domain.model.SeatKind
import com.ceta.cinema.domain.model.SeatStatus

/** Where the aisles cut a row into blocks: one place for the seat map drawing and for the lone-seat check. */
object SeatBlocks {
    /** The first column after each aisle (a 12-column room: 3 and 11, so the blocks are 1-2, 3-10, 11-12); none in a narrow room. */
    fun aisleStarts(maxColumn: Int): List<Int> {
        if (maxColumn < 8) return emptyList()
        val first = 3
        val last = if ((maxColumn - 1) % 2 == 1) maxColumn - 1 else maxColumn - 2
        return if (last > first) listOf(first, last) else listOf(first)
    }

    /** The first column of the block that [column] is in. */
    fun blockStart(column: Int, aisleStarts: List<Int>): Int = aisleStarts.lastOrNull { it <= column } ?: 1
}

/**
 * The soft warning of the seat map (design item 5): a free seat that would be left alone between the customer's choice and a
 * taken seat or an aisle, which nobody else can really use. It never blocks the booking.
 *
 * A free seat counts when, within its block of the row, both neighbours are "closed" (sold, held, chosen, missing, or the edge
 * of the block) and at least one of them is a chosen seat. Couple rows are skipped: their seats come in pairs.
 */
object LoneSeats {
    fun find(seats: List<Seat>, selected: Set<Long>): List<Seat> {
        if (selected.isEmpty()) return emptyList()
        val aisles = SeatBlocks.aisleStarts(seats.maxOfOrNull { it.column } ?: 0)
        val found = ArrayList<Seat>()
        for ((_, inRow) in seats.groupBy { it.row }) {
            if (inRow.any { it.kind == SeatKind.COUPLE }) continue
            val byColumn = inRow.filter { it.status != SeatStatus.INACTIVE }.associateBy { it.column }
            for (seat in byColumn.values) {
                if (!seat.selectable || seat.id in selected) continue
                val start = SeatBlocks.blockStart(seat.column, aisles)
                val end = aisles.firstOrNull { it > seat.column }?.minus(1) ?: Int.MAX_VALUE
                val left = byColumn[seat.column - 1]?.takeIf { seat.column - 1 >= start }
                val right = byColumn[seat.column + 1]?.takeIf { seat.column + 1 <= end }
                val leftClosed = left == null || !left.selectable || left.id in selected
                val rightClosed = right == null || !right.selectable || right.id in selected
                if (leftClosed && rightClosed && ((left != null && left.id in selected) || (right != null && right.id in selected))) found += seat
            }
        }
        return found.sortedWith(compareBy({ it.row }, { it.column }))
    }
}
