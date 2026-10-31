package io.github.prithvimk.gymtracker.ui.workout

import io.github.prithvimk.gymtracker.data.db.dao.WorkoutExerciseEntry
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeypadContextTest {
    private val workout = Workout("w", null, 0L, null, "", 0L, 0L)
    private val bench = entry("a", "bench", "Bench Press")
    private val row = entry("b", "row", "Barbell Row")

    @Test
    fun `names the exercise, numbers working sets and pairs last time`() {
        val state =
            WorkoutUiState.Active(
                workout,
                listOf(bench, row),
                sets = mapOf(
                    "a" to listOf(set("s3", 2), set("s1", 0, SetType.WARMUP), set("s2", 1)),
                    "b" to listOf(set("r1", 0)),
                ),
                previousSets = mapOf(
                    "bench" to listOf(set("p1", 0, SetType.WARMUP), set("p2", 1), set("p3", 2, reps = 6))
                ),
            )

        val context = keypadContext(state, SetFieldRef("s3", SetField.REPS))

        assertEquals("Bench Press", context?.exerciseName)
        assertEquals(SetLabel.Number(2), context?.label)
        assertEquals(SetField.REPS, context?.field)
        assertEquals("p3", context?.lastTime?.id)
    }

    @Test
    fun `warm-ups keep their kind and a set with no counterpart has no last time`() {
        val state =
            WorkoutUiState.Active(workout, listOf(row), sets = mapOf("b" to listOf(set("r1", 0, SetType.WARMUP))))

        val context = keypadContext(state, SetFieldRef("r1", SetField.WEIGHT))

        assertEquals(SetLabel.Kind(SetType.WARMUP), context?.label)
        assertNull(context?.lastTime)
    }

    @Test
    fun `a set that is gone has no context`() {
        val state = WorkoutUiState.Active(workout, listOf(row), sets = mapOf("b" to listOf(set("r1", 0))))

        assertNull(keypadContext(state, SetFieldRef("missing", SetField.WEIGHT)))
    }

    private fun entry(id: String, exerciseId: String, name: String) =
        WorkoutExerciseEntry(id, exerciseId, 0, name, MuscleGroup.CHEST, Equipment.BARBELL, "", null, null, null)

    private fun set(id: String, position: Int, type: SetType = SetType.WORKING, reps: Int = 8) =
        WorkoutSet(id, "x", position, 60.0, reps, null, type, null, 0L, 0L)
}
