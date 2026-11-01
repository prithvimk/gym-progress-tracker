package io.github.prithvimk.gymtracker.domain

import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionComparisonTest {
    @Test
    fun `the top set is the heaviest completed working set, then the one with most reps`() {
        val sets =
            listOf(
                set(100.0, 5, type = SetType.WARMUP),
                set(80.0, 6),
                set(80.0, 8),
                set(90.0, 3, done = false),
                set(70.0, 12, type = SetType.DROP),
            )

        assertEquals(TopSet(80.0, 8), topSet(sets))
    }

    @Test
    fun `no top set without a completed set other than warm-ups`() {
        assertNull(topSet(listOf(set(60.0, 10, type = SetType.WARMUP), set(80.0, 8, done = false))))
        assertNull(topSet(emptyList()))
    }

    @Test
    fun `bodyweight sets compare by reps`() {
        assertEquals(TopSet(0.0, 15), topSet(listOf(set(0.0, 12), set(0.0, 15))))
    }

    @Test
    fun `top set changes are clear only when weight and reps agree`() {
        assertEquals(TopSetChange.SAME, change(TopSet(80.0, 8), TopSet(80.0, 8)))
        assertEquals(TopSetChange.BETTER, change(TopSet(82.5, 8), TopSet(80.0, 8)))
        assertEquals(TopSetChange.BETTER, change(TopSet(80.0, 9), TopSet(80.0, 8)))
        assertEquals(TopSetChange.WORSE, change(TopSet(80.0, 6), TopSet(80.0, 8)))
        assertEquals(TopSetChange.WORSE, change(TopSet(75.0, 6), TopSet(80.0, 8)))
        assertEquals(TopSetChange.MIXED, change(TopSet(85.0, 5), TopSet(80.0, 8)))
        assertEquals(TopSetChange.MIXED, change(TopSet(75.0, 10), TopSet(80.0, 8)))
    }

    @Test
    fun `sessions compare volume and the top set of each exercise done in both`() {
        val current =
            listOf(
                SessionExercise("bench", "Bench Press", listOf(set(82.5, 8), set(82.5, 7))),
                SessionExercise("fly", "Cable Fly", listOf(set(20.0, 12))),
                SessionExercise("ohp", "Overhead Press", listOf(set(50.0, 8))),
            )
        val previous =
            listOf(
                SessionExercise("ohp", "Overhead Press", listOf(set(50.0, 8))),
                SessionExercise("bench", "Bench Press", listOf(set(80.0, 8))),
                SessionExercise("dip", "Dip", listOf(set(0.0, 12))),
            )

        val comparison = compareSessions(current, previous)

        assertEquals(82.5 * 15 + 20.0 * 12 + 50.0 * 8, comparison.volumeKg, 0.001)
        assertEquals(50.0 * 8 + 80.0 * 8, comparison.previousVolumeKg, 0.001)
        assertEquals((comparison.volumeKg - 1_040.0) / 1_040.0, comparison.volumeChange!!, 1e-9)
        assertEquals(listOf("bench", "ohp"), comparison.exercises.map { it.exerciseId })
        assertEquals(TopSet(82.5, 8), comparison.exercises[0].topSet)
        assertEquals(TopSet(80.0, 8), comparison.exercises[0].previousTopSet)
    }

    @Test
    fun `an exercise done twice in a session counts the sets of both`() {
        val current =
            listOf(
                SessionExercise("bench", "Bench Press", listOf(set(80.0, 8))),
                SessionExercise("bench", "Bench Press", listOf(set(85.0, 5))),
            )
        val previous = listOf(SessionExercise("bench", "Bench Press", listOf(set(80.0, 8))))

        assertEquals(listOf(TopSet(85.0, 5)), compareSessions(current, previous).exercises.map { it.topSet })
    }

    @Test
    fun `a session that moved no weight before has no volume change`() {
        val comparison =
            compareSessions(
                listOf(SessionExercise("dip", "Dip", listOf(set(0.0, 15)))),
                listOf(SessionExercise("dip", "Dip", listOf(set(0.0, 12)))),
            )

        assertNull(comparison.volumeChange)
        assertFalse(comparison.isEmpty)
        assertTrue(compareSessions(emptyList(), emptyList()).isEmpty)
    }

    private fun change(top: TopSet, previous: TopSet) = ExerciseComparison("e", "E", top, previous).change

    private var nextId = 0

    private fun set(weightKg: Double, reps: Int, type: SetType = SetType.WORKING, done: Boolean = true) = WorkoutSet(
        id = "s${nextId++}",
        workoutExerciseId = "we",
        position = nextId,
        weightKg = weightKg,
        reps = reps,
        rpe = null,
        type = type,
        completedAt = if (done) 1L else null,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
