package io.github.prithvimk.gymtracker.ui.exercises

import io.github.prithvimk.gymtracker.data.db.dao.ExerciseHistoryRow
import io.github.prithvimk.gymtracker.data.workoutSet
import io.github.prithvimk.gymtracker.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class E1rmTrendTest {
    @Test
    fun `each session's best estimated 1RM, oldest first, from working sets of up to 12 reps`() {
        val rows =
            listOf(
                row("new", startedAt = 20L, weightKg = 100.0, reps = 3),
                row("new", startedAt = 20L, weightKg = 90.0, reps = 6),
                row("new", startedAt = 20L, weightKg = 140.0, reps = 1, type = SetType.WARMUP),
                row("old", startedAt = 10L, weightKg = 80.0, reps = 8),
                // Only warm-ups and long sets: no estimate for this session.
                row("light", startedAt = 15L, weightKg = 40.0, reps = 20),
            )

        val points = e1rmPoints(rows)

        assertEquals(listOf(10L, 20L), points.map { it.startedAt })
        assertEquals(80.0 * (1 + 8 / 30.0), points[0].kg, 1e-9)
        assertEquals(100.0 * (1 + 3 / 30.0), points[1].kg, 1e-9)
    }

    @Test
    fun `the change is against the last session at least 30 days old`() {
        val now = 100 * DAY
        val points =
            listOf(
                E1rmPoint(now - 60 * DAY, 90.0),
                E1rmPoint(now - 31 * DAY, 95.0),
                E1rmPoint(now - 10 * DAY, 98.0),
                E1rmPoint(now - DAY, 100.0),
            )

        assertEquals(5.0, e1rmChange(points, now)!!, 1e-9)
    }

    @Test
    fun `no change without a session that old, or when the latest is the old one`() {
        val now = 100 * DAY

        assertNull(e1rmChange(listOf(E1rmPoint(now - 20 * DAY, 90.0), E1rmPoint(now, 95.0)), now))
        assertNull(e1rmChange(listOf(E1rmPoint(now - 40 * DAY, 90.0)), now))
        assertNull(e1rmChange(emptyList(), now))
    }

    private fun row(workoutId: String, startedAt: Long, weightKg: Double, reps: Int, type: SetType = SetType.WORKING,) =
        ExerciseHistoryRow(
            workoutId = workoutId,
            workoutStartedAt = startedAt,
            workoutEndedAt = startedAt + 1L,
            routineName = null,
            set = workoutSet(
                "$workoutId-$weightKg-$reps",
                "we-$workoutId",
                weightKg = weightKg,
                reps = reps,
                type = type,
                completedAt = 1L
            ),
        )

    private companion object {
        const val DAY = 86_400_000L
    }
}
