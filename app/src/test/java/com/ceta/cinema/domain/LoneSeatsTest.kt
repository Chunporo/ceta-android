package com.ceta.cinema.domain

import com.ceta.cinema.domain.model.Seat
import com.ceta.cinema.domain.model.SeatKind
import com.ceta.cinema.domain.model.SeatStatus
import com.ceta.cinema.domain.usecase.LoneSeats
import com.ceta.cinema.domain.usecase.SeatBlocks
import org.junit.Assert.*
import org.junit.Test

class LoneSeatsTest {
    /** One row of 12 seats: blocks are columns 1-2, 3-10 and 11-12 (aisles before 3 and 11). Seat id = column. */
    private fun row(sold: Set<Int> = emptySet(), kind: SeatKind = SeatKind.STANDARD, rowNo: Int = 1) = (1..12).map { c ->
        Seat(rowNo * 100L + c, "G$c", rowNo, c, kind, if (c in sold) SeatStatus.SOLD else SeatStatus.AVAILABLE, 85_000)
    }

    private fun lone(seats: List<Seat>, vararg chosen: Int, rowNo: Int = 1) =
        LoneSeats.find(seats, chosen.map { rowNo * 100L + it }.toSet()).map { it.column }

    @Test fun blocksFollowTheAisles() {
        assertEquals(listOf(3, 11), SeatBlocks.aisleStarts(12))
        assertEquals(emptyList<Int>(), SeatBlocks.aisleStarts(6))
        assertEquals(1, SeatBlocks.blockStart(2, listOf(3, 11)))
        assertEquals(3, SeatBlocks.blockStart(10, listOf(3, 11)))
        assertEquals(11, SeatBlocks.blockStart(12, listOf(3, 11)))
    }

    @Test fun nothingChosenMeansNoWarning() = assertEquals(emptyList<Int>(), lone(row(sold = setOf(2, 4)))) // gaps exist but nothing was chosen

    @Test fun aSeatBetweenTheBlockEdgeAndTheChoiceIsLone() {
        assertEquals(listOf(3), lone(row(), 4))      // 3 is squeezed between the aisle and seat 4
        assertEquals(listOf(1), lone(row(), 2))
        assertEquals(listOf(2), lone(row(), 1))      // block 1-2: seat 2 is left between seat 1 and the aisle
    }

    @Test fun aSeatBetweenTwoChosenSeatsIsLone() = assertEquals(listOf(4), lone(row(), 3, 5))

    @Test fun aSeatBetweenTheChoiceAndASoldSeatIsLone() = assertEquals(listOf(3, 5), lone(row(sold = setOf(6)), 4))

    @Test fun aSeatNextToTheChoiceWithRoomOnTheOtherSideIsFine() {
        assertEquals(emptyList<Int>(), lone(row(), 5))
        assertEquals(emptyList<Int>(), lone(row(), 3, 4))     // 5 still has 6 free beside it
        assertEquals(emptyList<Int>(), lone(row(), 10))       // 11 is in the next block, so it is not squeezed by seat 10
    }

    @Test fun aSeatTheCustomerCannotBookIsNeverTheWarning() = assertEquals(emptyList<Int>(), lone(row(sold = setOf(3)), 4)) // 3 is sold, not free

    @Test fun coupleRowsAreSkipped() = assertEquals(emptyList<Int>(), lone(row(kind = SeatKind.COUPLE, rowNo = 9), 4, rowNo = 9))

    @Test fun anotherRowDoesNotInterfere() {
        val seats = row(rowNo = 1) + row(rowNo = 2)
        assertEquals(listOf(3), lone(seats, 4, rowNo = 2))
        assertEquals(emptyList<Int>(), LoneSeats.find(seats, emptySet()).map { it.column })
    }
}
