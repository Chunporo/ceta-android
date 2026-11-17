package com.ceta.cinema.domain.usecase

import com.ceta.cinema.domain.model.Ticket
import java.time.Duration
import java.time.Instant

/** The tickets of one order: one card in the list, one QR per seat inside it. */
data class TicketGroup(val bookingId: Long, val tickets: List<Ticket>) {
    val lead: Ticket get() = tickets.first()
    val seats: List<String> get() = tickets.map { it.seatLabel }
    val allUsed: Boolean get() = tickets.all { it.used }
}

object TicketGrouping {
    /** A show counts as over once it has ended (or two hours after it started when the end is unknown). */
    fun isOver(group: TicketGroup, now: Instant): Boolean =
        !(group.lead.end ?: group.lead.start.plus(Duration.ofHours(2))).isAfter(now)

    fun group(tickets: List<Ticket>): List<TicketGroup> =
        tickets.groupBy { it.bookingId }.map { (id, list) -> TicketGroup(id, list.sortedBy { it.seatLabel }) }

    /** Upcoming first-to-start at the top; watched most recent at the top. */
    fun split(groups: List<TicketGroup>, now: Instant): Pair<List<TicketGroup>, List<TicketGroup>> {
        val (over, upcoming) = groups.partition { isOver(it, now) }
        return upcoming.sortedBy { it.lead.start } to over.sortedByDescending { it.lead.start }
    }
}
