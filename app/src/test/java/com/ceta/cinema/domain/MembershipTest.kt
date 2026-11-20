package com.ceta.cinema.domain

import com.ceta.cinema.data.repository.mockExpiring
import com.ceta.cinema.data.repository.mockMemberCode
import com.ceta.cinema.domain.usecase.MemberTier
import com.ceta.cinema.domain.usecase.MembershipPolicy
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MembershipTest {
    @Test fun tiersFollowTheSpend() {
        assertEquals(MemberTier.MEMBER, MembershipPolicy.tierOf(0))
        assertEquals(MemberTier.MEMBER, MembershipPolicy.tierOf(2_499_999))
        assertEquals(MemberTier.STAR, MembershipPolicy.tierOf(2_500_000))
        assertEquals(MemberTier.VIP, MembershipPolicy.tierOf(5_000_000))
    }

    @Test fun theSpendIsWorkedOutFromEarnedPointsAndVipMatchesTheBackendThreshold() {
        assertEquals(MemberTier.VIP, MembershipPolicy.tierOf(MembershipPolicy.spendOf(5_000)))  // the backend makes VIP at 5,000 earned points
        assertEquals(MemberTier.STAR, MembershipPolicy.tierOf(MembershipPolicy.spendOf(2_500)))
    }

    @Test fun theBarHasThreeMilestones() {
        assertEquals(0f, MembershipPolicy.progress(0), 0.001f)
        assertEquals(0.25f, MembershipPolicy.progress(1_250_000), 0.001f)
        assertEquals(0.5f, MembershipPolicy.progress(2_500_000), 0.001f)
        assertEquals(0.75f, MembershipPolicy.progress(3_750_000), 0.001f)
        assertEquals(1f, MembershipPolicy.progress(9_000_000), 0.001f)
    }

    @Test fun missingSpendIsWhatIsLeftForTheNextTier() {
        assertEquals(1_680_000L, MembershipPolicy.missing(820_000))
        assertEquals(1_275_000L, MembershipPolicy.missing(3_725_000))
        assertEquals(0L, MembershipPolicy.missing(6_000_000))
        assertNull(MembershipPolicy.next(MemberTier.VIP))
    }

    @Test fun theMockCodeIsStableAndLooksLikeTheDesign() {
        assertEquals(mockMemberCode("a@b.c"), mockMemberCode("a@b.c"))
        assertTrue(Regex("\\d{4} \\d{4}").matches(mockMemberCode("a@b.c")))
    }

    @Test fun expiringPointsAreAFlatShareOfTheBalanceOnNewYearsEve() {
        val e = mockExpiring(2_450, LocalDate.of(2026, 11, 20))!!
        assertEquals(120L, e.points)
        assertEquals(LocalDate.of(2026, 12, 31), e.on)
        assertNull(mockExpiring(150, LocalDate.of(2026, 11, 20)))
    }
}
