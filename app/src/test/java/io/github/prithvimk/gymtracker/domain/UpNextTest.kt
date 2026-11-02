package io.github.prithvimk.gymtracker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpNextTest {
    private data class R(val name: String, override val lastDoneAt: Long?, override val exerciseCount: Int = 3) :
        RoutineRotation

    @Test
    fun `the routine done least recently is up next`() {
        assertEquals("Legs", upNext(listOf(R("Pull", 300L), R("Legs", 100L), R("Push", 200L)))?.name)
    }

    @Test
    fun `a routine never done comes before any done, the first in the list winning ties`() {
        val routines = listOf(R("Pull", 100L), R("Arms", null), R("Core", null), R("Legs", 100L))

        assertEquals("Arms", upNext(routines)?.name)
        assertEquals("Pull", upNext(listOf(R("Pull", 100L), R("Legs", 100L)))?.name)
    }

    @Test
    fun `routines with no exercises are never suggested`() {
        assertEquals("Pull", upNext(listOf(R("Empty", null, exerciseCount = 0), R("Pull", 100L)))?.name)
        assertNull(upNext(listOf(R("Empty", null, exerciseCount = 0))))
        assertNull(upNext(emptyList<R>()))
    }
}
