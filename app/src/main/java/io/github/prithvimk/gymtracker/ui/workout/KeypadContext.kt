package io.github.prithvimk.gymtracker.ui.workout

import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.matchPrevious

/**
 * What the keypad is typing into, for the line along its top: the exercise, the set's label, the
 * field, and the set's counterpart from last session if there is one.
 */
data class KeypadContext(val exerciseName: String, val label: SetLabel, val field: SetField, val lastTime: WorkoutSet?,)

/** The context of [target] in [state], or null if the set is not in the workout. */
fun keypadContext(state: WorkoutUiState.Active, target: SetFieldRef): KeypadContext? {
    val entry = state.exercises.firstOrNull { e -> state.sets[e.id].orEmpty().any { it.id == target.setId } }
        ?: return null
    // In position order, as the set table numbers and pairs them.
    val sets = state.sets[entry.id].orEmpty().sortedBy { it.position }
    val index = sets.indexOfFirst { it.id == target.setId }
    val label = setLabels(sets.map { it.type })[index]
    val lastTime = matchPrevious(sets, state.previousSets[entry.exerciseId].orEmpty())[index]
    return KeypadContext(entry.name, label, target.field, lastTime)
}
