package com.ceta.cinema.domain

import com.ceta.cinema.domain.model.Seat
import com.ceta.cinema.domain.model.SeatKind
import com.ceta.cinema.domain.model.SeatStatus
import com.ceta.cinema.domain.usecase.SeatSelection
import com.ceta.cinema.domain.usecase.SeatSelection.Outcome
import com.ceta.cinema.domain.usecase.priceTags
import org.junit.Assert.*
import org.junit.Test

class SeatSelectionTest {
    private val rules = SeatSelection()

    private fun seat(id: Long, row: Int, col: Int, kind: SeatKind = SeatKind.STANDARD, status: SeatStatus = SeatStatus.AVAILABLE, price: Long = 85_000) =
        Seat(id, "${'A' + row - 1}$col", row, col, kind, status, price)

    private val row = (1..10).map { seat(it.toLong(), 1, it) }

    private fun changed(o: Outcome) = (o as Outcome.Changed).selected

    @Test fun tappingAFreeSeatSelectsAndTappingAgainDeselects() {
        val one = changed(rules.toggle(row, emptySet(), 3))
        assertEquals(setOf(3L), one)
        assertEquals(emptySet<Long>(), changed(rules.toggle(row, one, 3)))
    }

    @Test fun heldSoldAndInactiveSeatsDoNothing() {
        val seats = listOf(seat(1, 1, 1, status = SeatStatus.HELD), seat(2, 1, 2, status = SeatStatus.SOLD), seat(3, 1, 3, status = SeatStatus.INACTIVE),
            seat(4, 1, 4, status = SeatStatus.HELD_BY_ME))
        seats.forEach { assertEquals(Outcome.Ignored, rules.toggle(seats, emptySet(), it.id)) }
    }

    @Test fun anUnknownSeatIsIgnored() = assertEquals(Outcome.Ignored, rules.toggle(row, emptySet(), 999))

    @Test fun aNinthSeatIsRefusedAndTheSelectionStaysAsItWas() {
        var selected = emptySet<Long>()
        (1L..8L).forEach { selected = changed(rules.toggle(row + seat(11, 2, 1), selected, it)) }
        assertEquals(8, selected.size)
        assertEquals(Outcome.TooMany, rules.toggle(row + seat(11, 2, 1), selected, 9))
    }

    @Test fun anAlreadySelectedSeatCanAlwaysBeDeselectedEvenAtTheLimit() {
        val selected = (1L..8L).toSet()
        assertEquals(7, changed(rules.toggle(row, selected, 8)).size)
    }

    // couple seats: columns (1,2) (3,4) ... of one row
    private val couples = listOf(
        seat(1, 9, 1, SeatKind.COUPLE, price = 110_000), seat(2, 9, 2, SeatKind.COUPLE, price = 110_000),
        seat(3, 9, 3, SeatKind.COUPLE, price = 110_000), seat(4, 9, 4, SeatKind.COUPLE, price = 110_000),
    )

    @Test fun aCoupleSeatSelectsItsPartnerToo() {
        assertEquals(setOf(1L, 2L), changed(rules.toggle(couples, emptySet(), 1)))
        assertEquals(setOf(3L, 4L), changed(rules.toggle(couples, emptySet(), 4)))
    }

    @Test fun deselectingOneHalfDeselectsTheWholePair() {
        val both = changed(rules.toggle(couples, emptySet(), 1))
        assertEquals(emptySet<Long>(), changed(rules.toggle(couples, both, 2)))
    }

    @Test fun aPairIsRefusedWhenItsPartnerIsTaken() {
        val seats = listOf(seat(1, 9, 1, SeatKind.COUPLE), seat(2, 9, 2, SeatKind.COUPLE, status = SeatStatus.SOLD))
        assertEquals(Outcome.CouplePartnerTaken, rules.toggle(seats, emptySet(), 1))
    }

    @Test fun aLoneCoupleSeatWithoutAPartnerCannotBeBought() {
        assertEquals(Outcome.CouplePartnerTaken, rules.toggle(listOf(seat(1, 9, 1, SeatKind.COUPLE)), emptySet(), 1))
    }

    @Test fun aPairCountsAsTwoAgainstTheLimit() {
        val seats = row + couples.map { it.copy(id = it.id + 100) }
        val sevenSingles = (1L..7L).toSet()
        // 7 + a pair of 2 would be 9
        assertEquals(Outcome.TooMany, rules.toggle(seats, sevenSingles, 101))
        // 6 + a pair is exactly 8
        assertEquals(8, changed(rules.toggle(seats, (1L..6L).toSet(), 101)).size)
    }

    @Test fun twoPairsPlusFourSingles() {
        val seats = row + couples.map { it.copy(id = it.id + 100) }
        var selected = changed(rules.toggle(seats, emptySet(), 101))
        selected = changed(rules.toggle(seats, selected, 103))
        (1L..4L).forEach { selected = changed(rules.toggle(seats, selected, it)) }
        assertEquals(8, selected.size)
        assertEquals(Outcome.TooMany, rules.toggle(seats, selected, 5))
    }

    @Test fun totalIsTheSumOfThePricesOfTheChosenSeats() {
        val seats = row.take(2) + couples.map { it.copy(id = it.id + 100) }
        assertEquals(85_000L * 2 + 110_000L * 2, rules.total(seats, setOf(1L, 2L, 101L, 102L)))
        assertEquals(85_000L, rules.total(seats, setOf(1L)))
        assertEquals(0L, rules.total(seats, emptySet()))
    }

    @Test fun reconcileDropsSeatsSomeoneElseTookAndSaysWhich() {
        val selected = setOf(1L, 2L, 3L)
        val refreshed = row.map { if (it.id == 2L) it.copy(status = SeatStatus.HELD) else it }

        val (kept, lost) = rules.reconcile(refreshed, selected)

        assertEquals(setOf(1L, 3L), kept)
        assertEquals(listOf("A2"), lost.map { it.label })
    }

    @Test fun reconcileDropsBothHalvesOfAPairWhenOneIsTaken() {
        val refreshed = couples.map { if (it.id == 2L) it.copy(status = SeatStatus.SOLD) else it }
        val (kept, lost) = rules.reconcile(refreshed, setOf(1L, 2L))
        assertTrue(kept.isEmpty())
        assertEquals(listOf("I2"), lost.map { it.label })
    }

    @Test fun reconcileLeavesAnUntouchedSelectionAlone() {
        val (kept, lost) = rules.reconcile(row, setOf(1L, 2L))
        assertEquals(setOf(1L, 2L), kept)
        assertTrue(lost.isEmpty())
    }

    @Test fun reconcileDropsSeatsThatVanishedFromTheMap() {
        val (kept, _) = rules.reconcile(row.drop(1), setOf(1L, 2L))
        assertEquals(setOf(2L), kept)
    }

    @Test fun priceTagsUseTheCheapestSeatOfEachKind() {
        val seats = listOf(seat(1, 1, 1, price = 85_000), seat(2, 1, 2, price = 97_750), seat(3, 5, 1, SeatKind.VIP, price = 105_000),
            seat(4, 9, 1, SeatKind.COUPLE, price = 110_000), seat(5, 1, 3, price = 0, status = SeatStatus.INACTIVE))
        val tags = seats.priceTags()
        assertEquals(85_000L, tags[SeatKind.STANDARD])
        assertEquals(105_000L, tags[SeatKind.VIP])
        assertEquals(110_000L, tags[SeatKind.COUPLE])
    }
}
