package com.ceta.cinema.core

import com.ceta.cinema.data.local.prefs.SettingsStore
import com.ceta.cinema.data.local.prefs.withRecent
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRulesTest {
    @Test fun newestSearchComesFirst() = assertEquals(listOf("dune", "mario"), listOf("mario").withRecent("dune"))

    @Test fun aRepeatMovesToTheFrontWithoutDuplicates() =
        assertEquals(listOf("Dune", "mario"), listOf("mario", "dune").withRecent("Dune"))

    @Test fun blankQueriesAreIgnored() = assertEquals(listOf("a"), listOf("a").withRecent("   "))

    @Test fun onlyTheMostRecentAreKept() {
        val many = (1..10).map { "q$it" }.fold(emptyList<String>()) { acc, q -> acc.withRecent(q) }
        assertEquals(SettingsStore.MAX_RECENT, many.size)
        assertEquals("q10", many.first())
    }

    @Test fun surroundingSpacesAreTrimmed() = assertEquals(listOf("dune"), emptyList<String>().withRecent("  dune "))
}
