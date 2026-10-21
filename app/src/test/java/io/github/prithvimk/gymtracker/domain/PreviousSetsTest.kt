package io.github.prithvimk.gymtracker.domain

import io.github.prithvimk.gymtracker.data.workoutSet
import io.github.prithvimk.gymtracker.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviousSetsTest {
    private val lastTime =
        listOf(
            workoutSet("p0", "old", position = 0, weightKg = 40.0, reps = 10, type = SetType.WARMUP),
            workoutSet("p1", "old", position = 1, weightKg = 80.0, reps = 8),
            workoutSet("p2", "old", position = 2, weightKg = 80.0, reps = 7),
            workoutSet("p3", "old", position = 3, weightKg = 60.0, reps = 12, type = SetType.DROP),
        )

    @Test
    fun `sets match last time's set of the same type and place`() {
        val current =
            listOf(
                workoutSet("c0", "new", position = 0, type = SetType.WARMUP),
                workoutSet("c1", "new", position = 1),
                workoutSet("c2", "new", position = 2),
                workoutSet("c3", "new", position = 3),
                workoutSet("c4", "new", position = 4, type = SetType.DROP),
            )

        assertEquals(listOf("p0", "p1", "p2", null, "p3"), matchPrevious(current, lastTime).map { it?.id })
    }

    @Test
    fun `an extra warm-up does not shift the working sets`() {
        val current =
            listOf(
                workoutSet("c0", "new", position = 0, type = SetType.WARMUP),
                workoutSet("c1", "new", position = 1, type = SetType.WARMUP),
                workoutSet("c2", "new", position = 2),
            )

        assertEquals(listOf("p0", null, "p1"), matchPrevious(current, lastTime).map { it?.id })
    }

    @Test
    fun `previous sets without reps are skipped`() {
        val previous =
            listOf(
                workoutSet("blank", "old", position = 0, reps = 0),
                workoutSet("done", "old", position = 1, reps = 5),
            )

        assertEquals(listOf("done"), matchPrevious(listOf(workoutSet("c", "new")), previous).map { it?.id })
    }

    @Test
    fun `no previous session matches nothing`() {
        assertEquals(listOf(null), matchPrevious(listOf(workoutSet("c", "new")), emptyList()))
    }

    @Test
    fun `a new set starts from the next working set of last time`() {
        val current = listOf(workoutSet("c0", "new", position = 0, weightKg = 85.0, reps = 6))

        assertEquals(SetPrefill(80.0, 7), prefillForNewSet(current, lastTime))
        assertEquals(SetPrefill(80.0, 8), prefillForNewSet(emptyList(), lastTime))
    }

    @Test
    fun `beyond last time a new set copies the set before it`() {
        val current =
            listOf(
                workoutSet("c0", "new", position = 0, weightKg = 80.0, reps = 8),
                workoutSet("c1", "new", position = 1, weightKg = 82.5, reps = 6),
                workoutSet("c2", "new", position = 2, weightKg = 85.0, reps = 5),
            )

        assertEquals(SetPrefill(85.0, 5), prefillForNewSet(current, lastTime))
    }

    @Test
    fun `the first set ever is blank`() {
        assertEquals(SetPrefill(0.0, 0), prefillForNewSet(emptyList(), emptyList()))
    }
}
