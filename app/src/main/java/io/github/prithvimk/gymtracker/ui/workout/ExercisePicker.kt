package io.github.prithvimk.gymtracker.ui.workout

import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseFilter
import io.github.prithvimk.gymtracker.ui.exercises.filteredBy

/** A chip over the add-exercises list: what it narrows the list to. */
sealed interface PickerChip {
    /** Exercises from the last few finished workouts, most recent first. */
    data object Recent : PickerChip

    data object All : PickerChip

    data class Muscle(val group: MuscleGroup) : PickerChip
}

/**
 * The add-exercises sheet. [selectedIds] keeps tap order, which is the order the exercises are added
 * in, and [picks] holds those exercises for the tray. [chip] is null until one is tapped, which means
 * Recent if there are recent exercises and All if not. [options] is the list under the chips.
 */
data class ExercisePickerState(
    val query: String = "",
    val selectedIds: List<String> = emptyList(),
    val chip: PickerChip? = null,
    val hasRecent: Boolean = false,
    val options: List<Exercise> = emptyList(),
    val picks: List<Exercise> = emptyList(),
) {
    /** The chip that is on: the one tapped, or the default. */
    val shownChip: PickerChip get() = chip ?: if (hasRecent) PickerChip.Recent else PickerChip.All
}

/**
 * Fills in the sheet from the library [all] and the [recentIds] (most recent first). A search under
 * Recent looks through the whole library, since the recent list is short; under a muscle group it
 * stays within that group. Archived exercises are never offered.
 */
fun ExercisePickerState.withLibrary(all: List<Exercise>, recentIds: List<String>): ExercisePickerState {
    val byId = all.associateBy { it.id }
    val recent = recentIds.distinct().mapNotNull { byId[it] }.filterNot { it.archived }
    val resolved = copy(hasRecent = recent.isNotEmpty())
    val options =
        when (val chip = resolved.shownChip) {
            PickerChip.Recent ->
                if (query.isBlank()) recent else all.filteredBy(ExerciseFilter(query = query))

            PickerChip.All -> all.filteredBy(ExerciseFilter(query = query))
            is PickerChip.Muscle -> all.filteredBy(ExerciseFilter(query = query, muscleGroup = chip.group))
        }
    return resolved.copy(options = options, picks = selectedIds.mapNotNull { byId[it] })
}
