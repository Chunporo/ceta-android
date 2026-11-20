package com.ceta.cinema.domain.usecase

import java.time.LocalDate

/** The three card tiers of the design. The backend knows only MEMBER and VIP; STAR sits between them (see [MembershipPolicy]). */
enum class MemberTier { MEMBER, STAR, VIP }

/**
 * Which tier a customer is on and how far along they are.
 *
 * TODO(business): the design's thresholds are assumptions: STAR from 2,500,000đ and VIP from 5,000,000đ spent in the year, kept
 * until the end of the next year. The backend has no yearly spend: it earns 1 point per 1,000đ paid and makes VIP at 5,000 earned
 * points, so the spend here is `lifetimeEarned × 1,000` (the same number for VIP, and STAR is simply half). Replace [spendOf] with
 * a real yearly figure and let the backend say the tier.
 */
object MembershipPolicy {
    const val STAR_SPEND = 2_500_000L
    const val VIP_SPEND = 5_000_000L
    private const val VND_PER_POINT = 1_000L

    fun spendOf(lifetimeEarned: Long): Long = lifetimeEarned * VND_PER_POINT

    fun tierOf(spend: Long): MemberTier = when {
        spend >= VIP_SPEND -> MemberTier.VIP
        spend >= STAR_SPEND -> MemberTier.STAR
        else -> MemberTier.MEMBER
    }

    fun next(tier: MemberTier): MemberTier? = when (tier) { MemberTier.MEMBER -> MemberTier.STAR; MemberTier.STAR -> MemberTier.VIP; MemberTier.VIP -> null }

    /** Spend still missing for the next tier; 0 at the top. */
    fun missing(spend: Long): Long = when (tierOf(spend)) {
        MemberTier.MEMBER -> STAR_SPEND - spend
        MemberTier.STAR -> VIP_SPEND - spend
        MemberTier.VIP -> 0
    }

    /** Position on the three-milestone bar, 0..1: MEMBER at 0, STAR at half, VIP at 1. */
    fun progress(spend: Long): Float = when (tierOf(spend)) {
        MemberTier.MEMBER -> 0.5f * spend.coerceAtLeast(0) / STAR_SPEND
        MemberTier.STAR -> 0.5f + 0.5f * (spend - STAR_SPEND) / (VIP_SPEND - STAR_SPEND)
        MemberTier.VIP -> 1f
    }
}

/** A line of the points history: points earned (+), spent or given back (-). */
data class PointsEntry(val kind: Kind, val movie: String?, val date: LocalDate?, val points: Long) {
    enum class Kind { EARN, REFUND }
}

data class ExpiringPoints(val points: Long, val on: LocalDate)
