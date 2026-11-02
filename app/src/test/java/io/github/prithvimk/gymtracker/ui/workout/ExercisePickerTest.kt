package io.github.prithvimk.gymtracker.ui.workout

import io.github.prithvimk.gymtracker.data.exercise
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExercisePickerTest {
    private val library =
        listOf(
            exercise(id = "bench", name = "Bench Press", muscleGroup = MuscleGroup.CHEST),
            exercise(id = "fly", name = "Cable Fly", muscleGroup = MuscleGroup.CHEST),
            exercise(id = "ohp", name = "Overhead Press", muscleGroup = MuscleGroup.SHOULDERS),
            exercise(id = "old", name = "Old Press", muscleGroup = MuscleGroup.SHOULDERS, archived = true),
            exercise(id = "row", name = "Barbell Row", muscleGroup = MuscleGroup.BACK),
        )

    private fun ids(state: ExercisePickerState) = state.options.map { it.id }

    @Test
    fun `Recent is on by default and lists recent exercises, most recent first, without archived ones`() {
        val state = ExercisePickerState().withLibrary(library, listOf("ohp", "old", "bench", "ohp"))

        assertTrue(state.hasRecent)
        assertEquals(PickerChip.Recent, state.shownChip)
        assertEquals(listOf("ohp", "bench"), ids(state))
    }

    @Test
    fun `with nothing recent, All is on`() {
        val state = ExercisePickerState().withLibrary(library, emptyList())

        assertFalse(state.hasRecent)
        assertEquals(PickerChip.All, state.shownChip)
        assertEquals(listOf("bench", "fly", "ohp", "row"), ids(state))
    }

    @Test
    fun `a search under Recent looks through the whole library, under a muscle only in it`() {
        val recent = listOf("bench")

        assertEquals(listOf("bench", "ohp"), ids(ExercisePickerState(query = "press").withLibrary(library, recent)))
        assertEquals(
            listOf("ohp"),
            ids(
                ExercisePickerState(query = "press", chip = PickerChip.Muscle(MuscleGroup.SHOULDERS))
                    .withLibrary(library, recent),
            ),
        )
    }

    @Test
    fun `picks come out in tick order for the tray`() {
        val state = ExercisePickerState(selectedIds = listOf("row", "bench")).withLibrary(library, emptyList())

        assertEquals(listOf("row", "bench"), state.picks.map { it.id })
    }
}
