package com.ceta.cinema.data.repository

import com.ceta.cinema.core.*
import com.ceta.cinema.data.remote.api.CetaApi
import com.ceta.cinema.data.remote.mapper.toDomain
import com.ceta.cinema.domain.model.Ticket
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/** Tickets are kept for 5 minutes so a QR still opens when the signal is weak at the cinema. */
@Singleton
class TicketRepository @Inject constructor(private val api: CetaApi, private val offline: OfflineFlag) {
    private val list = TtlCache<Unit, List<Ticket>>(Duration.ofMinutes(5))

    suspend fun myTickets(forceRefresh: Boolean = false): ApiResult<List<Ticket>> {
        if (forceRefresh) list.remove(Unit)
        return cachedCall(list, Unit, offline) { api.myTickets(page = 0, size = 50).items.map { it.toDomain() } }
    }

    /** Called when a payment completes, so the new tickets show up without waiting for the cache to age out. */
    fun forget() = list.clear()

    suspend fun ticket(id: Long): ApiResult<Ticket> = safeApiCall { api.ticket(id).toDomain() }
}
