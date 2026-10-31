package io.github.prithvimk.gymtracker.ui.history

import io.github.prithvimk.gymtracker.data.db.dao.WorkoutExerciseEntry
import io.github.prithvimk.gymtracker.data.workoutSet
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.domain.model.SetType
import io.github.prithvimk.gymtracker.ui.workout.SetLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class DoneExerciseTest {
    @Test
    fun `only done sets are kept, numbered among themselves, with their records`() {
        val sets =
            mapOf(
                "a" to
                    listOf(
                        workoutSet("warm", "a", position = 0, type = SetType.WARMUP, completedAt = 1L),
                        workoutSet("skipped", "a", position = 1, completedAt = null),
                        workoutSet("top", "a", position = 2, completedAt = 2L),
                        workoutSet("drop", "a", position = 3, type = SetType.DROP, completedAt = 3L),
                    ),
            )

        val done = doneExercises(listOf(entry("a", 0), entry("b", 1)), sets, mapOf("top" to listOf(PrType.WEIGHT)))

        assertEquals(listOf("a", "b"), done.map { it.entry.id })
        val first = done.first().sets
        assertEquals(listOf("warm", "top", "drop"), first.map { it.set.id })
        assertEquals(
            listOf(SetLabel.Kind(SetType.WARMUP), SetLabel.Number(1), SetLabel.Kind(SetType.DROP)),
            first.map { it.label },
        )
        assertEquals(listOf(emptyList(), listOf(PrType.WEIGHT), emptyList()), first.map { it.records })
        assertEquals(emptyList<Any>(), done.last().sets)
    }

    private fun entry(id: String, position: Int) = WorkoutExerciseEntry(
        id = id,
        exerciseId = "ex-$id",
        position = position,
        name = "Exercise $id",
        muscleGroup = MuscleGroup.CHEST,
        equipment = Equipment.BARBELL,
        notes = "",
        repMin = null,
        repMax = null,
        restSeconds = null,
    )
}
