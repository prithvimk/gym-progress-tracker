package io.github.prithvimk.gymtracker.domain

import io.github.prithvimk.gymtracker.data.workoutExercise
import io.github.prithvimk.gymtracker.data.workoutSet
import io.github.prithvimk.gymtracker.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutinePlanTest {
    @Test
    fun `normalized clamps targets and puts the rep range the right way round`() {
        assertEquals(ExerciseTargets(1, 6, 10, 0), ExerciseTargets(0, 10, 6, -30).normalized())
        assertEquals(
            ExerciseTargets(MAX_TARGET_SETS, 1, MAX_TARGET_REPS, MAX_REST_SECONDS),
            ExerciseTargets(99, 0, 500, 9_999).normalized(),
        )
        assertEquals(75, ExerciseTargets(3, 8, 12, 80).normalized().restSeconds)
    }

    @Test
    fun `plan follows exercise order and ignores warm-ups`() {
        val exercises =
            listOf(workoutExercise("b", "w", "ohp", position = 1), workoutExercise("a", "w", "bench", position = 0))
        val sets =
            listOf(
                workoutSet("1", "a", 0, reps = 12, type = SetType.WARMUP, completedAt = 1L),
                workoutSet("2", "a", 1, reps = 8, completedAt = 1L),
                workoutSet("3", "a", 2, reps = 10, type = SetType.DROP, completedAt = 1L),
            )

        val plan = planFromWorkout(exercises, sets)

        assertEquals(listOf("bench", "ohp"), plan.map { it.exerciseId })
        assertEquals(ExerciseTargets(2, 8, 10, DEFAULT_REST_SECONDS), plan[0].targets)
        assertEquals(ExerciseTargets.Default, plan[1].targets)
    }

    @Test
    fun `only completed sets count when some were completed`() {
        val sets =
            listOf(
                workoutSet("1", "a", 0, reps = 5, completedAt = 1L),
                workoutSet("2", "a", 1, reps = 15),
                workoutSet("3", "a", 2, reps = 15),
            )

        val targets = planFromWorkout(listOf(workoutExercise("a", "w", "bench")), sets).single().targets

        assertEquals(ExerciseTargets(1, 5, 5, DEFAULT_REST_SECONDS), targets)
    }

    @Test
    fun `all sets count when none were completed, and blank reps fall back to defaults`() {
        val sets = listOf(workoutSet("1", "a", 0, reps = 0), workoutSet("2", "a", 1, reps = 0))

        val targets = planFromWorkout(listOf(workoutExercise("a", "w", "bench")), sets).single().targets

        assertEquals(ExerciseTargets(2, DEFAULT_REP_MIN, DEFAULT_REP_MAX, DEFAULT_REST_SECONDS), targets)
    }

    @Test
    fun `targets copied from a routine are kept`() {
        val exercise = workoutExercise("a", "w", "bench").copy(repMin = 3, repMax = 5, restSeconds = 240)
        val sets = listOf(workoutSet("1", "a", 0, reps = 9, completedAt = 1L))

        val targets = planFromWorkout(listOf(exercise), sets).single().targets

        assertEquals(ExerciseTargets(1, 3, 5, 240), targets)
    }
}
