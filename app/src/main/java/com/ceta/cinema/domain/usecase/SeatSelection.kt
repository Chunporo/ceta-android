package com.ceta.cinema.domain.usecase

import com.ceta.cinema.domain.model.Seat
import com.ceta.cinema.domain.model.SeatKind
import com.ceta.cinema.domain.model.SeatStatus

/** The rules for choosing seats, apart from any screen. The server has the last word; this only avoids pointless requests. */
class SeatSelection(private val maxSeats: Int = DEFAULT_MAX_SEATS) {
    sealed interface Outcome {
        data class Changed(val selected: Set<Long>) : Outcome
        /** Tapping a seat that cannot be chosen does nothing, silently (no toast: it would be noise). */
        data object Ignored : Outcome
        data object TooMany : Outcome
        /** The other half of a couple seat is taken. */
        data object CouplePartnerTaken : Outcome
    }

    fun toggle(seats: List<Seat>, selected: Set<Long>, tappedId: Long): Outcome {
        val tapped = seats.firstOrNull { it.id == tappedId } ?: return Outcome.Ignored
        val group = group(seats, tapped)
        if (tapped.id in selected) return Outcome.Changed(selected - group.map { it.id }.toSet())
        if (!tapped.selectable) return Outcome.Ignored
        if (tapped.kind == SeatKind.COUPLE && (group.size < 2 || group.any { !it.selectable })) return Outcome.CouplePartnerTaken
        val next = selected + group.map { it.id }
        return if (next.size > maxSeats) Outcome.TooMany else Outcome.Changed(next)
    }

    /** A seat and, for a couple seat, its partner: columns (2k-1, 2k) of one row, which is how the server pairs them. */
    fun group(seats: List<Seat>, seat: Seat): List<Seat> {
        if (seat.kind != SeatKind.COUPLE) return listOf(seat)
        val partnerColumn = if (seat.column % 2 == 1) seat.column + 1 else seat.column - 1
        val partner = seats.firstOrNull { it.row == seat.row && it.column == partnerColumn && it.kind == SeatKind.COUPLE }
        return listOfNotNull(seat, partner)
    }

    /**
     * After a refresh: drop chosen seats that someone else took, and report which, so the screen can say so.
     * A seat the customer already holds for this very show is not "taken".
     */
    fun reconcile(seats: List<Seat>, selected: Set<Long>): Pair<Set<Long>, List<Seat>> {
        val byId = seats.associateBy { it.id }
        val lost = selected.mapNotNull { byId[it] }.filter { !it.selectable }
        val missing = selected - byId.keys
        val kept = selected - lost.map { it.id }.toSet() - missing
        // A couple seat is both or nothing.
        val partners = lost.filter { it.kind == SeatKind.COUPLE }.flatMap { group(seats, it) }.map { it.id }.toSet()
        return (kept - partners) to lost
    }

    fun total(seats: List<Seat>, selected: Set<Long>): Long = seats.filter { it.id in selected }.sumOf { it.price }

    companion object { const val DEFAULT_MAX_SEATS = 8 }
}

/** Cheapest price per kind, for the price tags under the map. */
fun List<Seat>.priceTags(): Map<SeatKind, Long> =
    filter { it.status != SeatStatus.INACTIVE && it.price > 0 }.groupBy { it.kind }.mapValues { (_, v) -> v.minOf { it.price } }
