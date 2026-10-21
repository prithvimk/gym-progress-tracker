package io.github.prithvimk.gymtracker.domain

import io.github.prithvimk.gymtracker.data.workout
import io.github.prithvimk.gymtracker.data.workoutSet
import io.github.prithvimk.gymtracker.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSummaryTest {
    @Test
    fun `volume is weight times reps summed over completed sets`() {
        val sets =
            listOf(
                workoutSet("a", "bench", weightKg = 80.0, reps = 8, completedAt = 1L),
                workoutSet("b", "bench", weightKg = 82.5, reps = 6, completedAt = 2L),
            )

        assertEquals(1_135.0, volumeKg(sets), 1e-9)
    }

    @Test
    fun `sets not marked done add no volume`() {
        val sets =
            listOf(
                workoutSet("a", "bench", weightKg = 80.0, reps = 8, completedAt = 1L),
                workoutSet("b", "bench", weightKg = 80.0, reps = 8),
            )

        assertEquals(640.0, volumeKg(sets), 1e-9)
    }

    @Test
    fun `warm-ups and drop sets count, bodyweight sets add nothing`() {
        val sets =
            listOf(
                workoutSet("a", "bench", weightKg = 40.0, reps = 10, type = SetType.WARMUP, completedAt = 1L),
                workoutSet("b", "bench", weightKg = 60.0, reps = 12, type = SetType.DROP, completedAt = 2L),
                workoutSet("c", "pullup", weightKg = 0.0, reps = 10, completedAt = 3L),
            )

        assertEquals(1_120.0, volumeKg(sets), 1e-9)
    }

    @Test
    fun `no sets is zero volume`() {
        assertEquals(0.0, volumeKg(emptyList()), 0.0)
    }

    @Test
    fun `summary counts completed sets and the exercises they belong to`() {
        val sets =
            listOf(
                workoutSet("a", "bench", position = 0, weightKg = 80.0, reps = 8, completedAt = 1L),
                workoutSet("b", "bench", position = 1, weightKg = 80.0, reps = 8, completedAt = 2L),
                workoutSet("c", "ohp", position = 0, weightKg = 50.0, reps = 5, completedAt = 3L),
                workoutSet("d", "fly", position = 0, weightKg = 15.0, reps = 12),
            )

        val summary = summarizeWorkout(workout("w", startedAt = 1_000L, endedAt = 3_601_000L), sets, now = 9_999_999L)

        assertEquals(
            WorkoutSummary(durationMillis = 3_600_000L, exerciseCount = 2, setCount = 3, volumeKg = 1_530.0),
            summary
        )
    }

    @Test
    fun `an unfinished workout runs until now`() {
        val summary = summarizeWorkout(workout("w", startedAt = 1_000L), emptyList(), now = 61_000L)

        assertEquals(WorkoutSummary(durationMillis = 60_000L, exerciseCount = 0, setCount = 0, volumeKg = 0.0), summary)
    }

    @Test
    fun `an end before the start counts as zero duration`() {
        val summary = summarizeWorkout(workout("w", startedAt = 5_000L, endedAt = 4_000L), emptyList(), now = 0L)

        assertEquals(0L, summary.durationMillis)
    }
}
