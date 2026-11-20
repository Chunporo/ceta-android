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

/** How far away an upcoming show is, for the accent chip on its card. */
sealed interface ShowWhen {
    data object Today : ShowWhen
    data object Tomorrow : ShowWhen
    data class InDays(val days: Long) : ShowWhen
}

/** A ticket the customer cancelled, with what was refunded. */
data class CancelledTicket(val group: TicketGroup, val refund: Long)

object TicketGrouping {
    /** Today / tomorrow / in n days, counted in the cinema's calendar days. */
    fun whenIs(start: Instant, now: Instant): ShowWhen {
        val days = java.time.temporal.ChronoUnit.DAYS.between(com.ceta.cinema.core.Formatters.localDate(now), com.ceta.cinema.core.Formatters.localDate(start))
        return when {
            days <= 0 -> ShowWhen.Today
            days == 1L -> ShowWhen.Tomorrow
            else -> ShowWhen.InDays(days)
        }
    }

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
