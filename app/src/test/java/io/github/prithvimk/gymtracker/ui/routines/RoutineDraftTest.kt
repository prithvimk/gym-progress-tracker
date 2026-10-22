package io.github.prithvimk.gymtracker.ui.routines

import io.github.prithvimk.gymtracker.domain.ExerciseTargets
import io.github.prithvimk.gymtracker.domain.MAX_REST_SECONDS
import io.github.prithvimk.gymtracker.domain.MAX_TARGET_SETS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineDraftTest {
    private val targets = ExerciseTargets(targetSets = 3, repMin = 8, repMax = 10, restSeconds = 90)

    @Test
    fun `sets and rest step by one and by 15 seconds`() {
        assertEquals(4, targets.stepped(TargetField.SETS, up = true).targetSets)
        assertEquals(2, targets.stepped(TargetField.SETS, up = false).targetSets)
        assertEquals(105, targets.stepped(TargetField.REST, up = true).restSeconds)
        assertEquals(75, targets.stepped(TargetField.REST, up = false).restSeconds)
    }

    @Test
    fun `steps stop at the limits`() {
        val low = ExerciseTargets(1, 1, 1, 0)
        assertEquals(low, low.stepped(TargetField.SETS, up = false))
        assertEquals(low, low.stepped(TargetField.REST, up = false))
        assertEquals(low, low.stepped(TargetField.REP_MIN, up = false))
        assertFalse(low.canStep(TargetField.SETS, up = false))
        assertTrue(low.canStep(TargetField.SETS, up = true))

        val high = ExerciseTargets(MAX_TARGET_SETS, 8, 12, MAX_REST_SECONDS)
        assertFalse(high.canStep(TargetField.SETS, up = true))
        assertFalse(high.canStep(TargetField.REST, up = true))
    }

    @Test
    fun `rep range ends push each other along instead of crossing`() {
        val tight = ExerciseTargets(3, 8, 8, 90)
        assertEquals(ExerciseTargets(3, 9, 9, 90), tight.stepped(TargetField.REP_MIN, up = true))
        assertEquals(ExerciseTargets(3, 7, 7, 90), tight.stepped(TargetField.REP_MAX, up = false))
        assertEquals(ExerciseTargets(3, 8, 11, 90), targets.stepped(TargetField.REP_MAX, up = true))
    }

    @Test
    fun `moved shifts an item and clamps at the ends`() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), list.moved(0, 1))
        assertEquals(listOf("a", "c", "b"), list.moved(2, -1))
        assertEquals(list, list.moved(0, -1))
        assertEquals(listOf("b", "c", "a"), list.moved(0, 5))
        assertEquals(list, list.moved(7, 1))
    }
}
