package io.github.prithvimk.gymtracker.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TopLevelDestinationTest {
    @Test
    fun `routes are unique`() {
        val routes = TopLevelDestination.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `fromRoute resolves every destination`() {
        TopLevelDestination.entries.forEach {
            assertEquals(it, TopLevelDestination.fromRoute(it.route))
        }
    }

    @Test
    fun `fromRoute returns null for unknown routes`() {
        assertNull(TopLevelDestination.fromRoute("nope"))
        assertNull(TopLevelDestination.fromRoute(null))
    }

    @Test
    fun `workout is the start destination`() {
        assertEquals(TopLevelDestination.WORKOUT, TopLevelDestination.START)
    }
}
