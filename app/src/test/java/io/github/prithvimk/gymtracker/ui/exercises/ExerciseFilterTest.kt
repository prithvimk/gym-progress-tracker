package io.github.prithvimk.gymtracker.ui.exercises

import io.github.prithvimk.gymtracker.data.exercise
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseFilterTest {
    private val exercises =
        listOf(
            exercise(id = "1", name = "Incline Barbell Bench Press", muscleGroup = MuscleGroup.CHEST),
            exercise(id = "2", name = "Lat Pulldown", muscleGroup = MuscleGroup.BACK),
            exercise(id = "3", name = "Cable Fly", muscleGroup = MuscleGroup.CHEST, archived = true),
        )

    private fun ids(criteria: ExerciseFilter) = exercises.filteredBy(criteria).map { it.id }

    @Test
    fun `default filter hides archived exercises`() {
        assertEquals(listOf("1", "2"), ids(ExerciseFilter()))
    }

    @Test
    fun `query words match in any order and case`() {
        assertEquals(listOf("1"), ids(ExerciseFilter(query = "bench INCLINE")))
        assertEquals(emptyList<String>(), ids(ExerciseFilter(query = "bench row")))
    }

    @Test
    fun `blank query matches everything visible`() {
        assertEquals(listOf("1", "2"), ids(ExerciseFilter(query = "   ")))
    }

    @Test
    fun `muscle group and show archived combine`() {
        val criteria = ExerciseFilter(muscleGroup = MuscleGroup.CHEST, showArchived = true)

        assertEquals(listOf("1", "3"), ids(criteria))
    }
}
