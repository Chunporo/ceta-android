package com.ceta.cinema.data.repository

import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.CinemaZone
import com.ceta.cinema.domain.model.BookingStatus
import com.ceta.cinema.domain.usecase.ExpiringPoints
import com.ceta.cinema.domain.usecase.PointsEntry
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/** What the membership card shows beyond the balance. */
interface MembershipRepository {
    /** The latest points movements, newest first. */
    suspend fun history(limit: Int = 3): List<PointsEntry>

    /** Points about to lapse, or null when none. */
    fun expiring(balance: Long, today: LocalDate): ExpiringPoints?

    /** The code shown as a QR at the counter, written as "0912 3456". */
    fun memberCode(seed: String): String
}

/**
 * TODO(backend): none of this has an endpoint yet. Wanted: `GET /me/loyalty/history` (the ledger the backend already keeps),
 * an expiry rule with the date on `/me/loyalty`, and a member code on the profile.
 * Until then: the history is worked out from the order history (a paid order earns total ÷ 1,000 points, a refunded one takes them
 * back), expiring points are 5 % of the balance on 31 December (made up), and the code is a stable number made from [seed].
 */
@Singleton
class MockMembershipRepository @Inject constructor(private val bookings: BookingRepository) : MembershipRepository {
    override suspend fun history(limit: Int): List<PointsEntry> {
        val orders = (bookings.history() as? ApiResult.Ok)?.data.orEmpty()
        return orders.mapNotNull { o ->
            val kind = when (o.status) { BookingStatus.PAID -> PointsEntry.Kind.EARN; BookingStatus.REFUNDED -> PointsEntry.Kind.REFUND; else -> return@mapNotNull null }
            val points = o.totalAmount / 1_000
            if (points <= 0) null else o to PointsEntry(kind, o.movieTitle, o.createdAt?.atZone(CinemaZone)?.toLocalDate(), if (kind == PointsEntry.Kind.EARN) points else -points)
        }.sortedByDescending { it.first.createdAt }.map { it.second }.take(limit)
    }

    override fun expiring(balance: Long, today: LocalDate): ExpiringPoints? = mockExpiring(balance, today)

    override fun memberCode(seed: String): String = mockMemberCode(seed)
}

internal fun mockExpiring(balance: Long, today: LocalDate): ExpiringPoints? {
    val points = (balance / 20 / 10) * 10
    return if (points > 0) ExpiringPoints(points, LocalDate.of(today.year, 12, 31)) else null
}

internal fun mockMemberCode(seed: String): String {
    val n = (abs(seed.hashCode().toLong()) % 100_000_000L).toString().padStart(8, '0')
    return n.substring(0, 4) + " " + n.substring(4)
}
