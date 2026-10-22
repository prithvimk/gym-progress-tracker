package io.github.prithvimk.gymtracker.ui.routines

import io.github.prithvimk.gymtracker.domain.ExerciseTargets
import io.github.prithvimk.gymtracker.domain.MAX_REST_SECONDS
import io.github.prithvimk.gymtracker.domain.MAX_TARGET_REPS
import io.github.prithvimk.gymtracker.domain.MAX_TARGET_SETS
import io.github.prithvimk.gymtracker.domain.REST_STEP_SECONDS
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup

/**
 * An exercise in the routine being edited. [key] identifies it in the list; [id] is its stored row,
 * or null if it was added in this edit.
 */
data class DraftExercise(
    val key: String,
    val id: String?,
    val exerciseId: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val targets: ExerciseTargets,
)

/** One of an exercise's targets, changed a step at a time in the editor. */
enum class TargetField { SETS, REP_MIN, REP_MAX, REST }

/**
 * The targets with [field] moved one step up or down, kept in range. Moving one end of the rep range
 * past the other drags the other along, so the range never turns inside out.
 */
fun ExerciseTargets.stepped(field: TargetField, up: Boolean): ExerciseTargets {
    val sign = if (up) 1 else -1
    return when (field) {
        TargetField.SETS -> copy(targetSets = (targetSets + sign).coerceIn(1, MAX_TARGET_SETS))
        TargetField.REP_MIN -> {
            val min = (repMin + sign).coerceIn(1, MAX_TARGET_REPS)
            copy(repMin = min, repMax = maxOf(repMax, min))
        }
        TargetField.REP_MAX -> {
            val max = (repMax + sign).coerceIn(1, MAX_TARGET_REPS)
            copy(repMin = minOf(repMin, max), repMax = max)
        }
        TargetField.REST -> copy(restSeconds = (restSeconds + sign * REST_STEP_SECONDS).coerceIn(0, MAX_REST_SECONDS))
    }
}

/** Whether [field] can move a step in that direction. */
fun ExerciseTargets.canStep(field: TargetField, up: Boolean): Boolean = stepped(field, up) != this

/** Moves the item at [index] by [offset] places, clamped to the list. */
fun <T> List<T>.moved(index: Int, offset: Int): List<T> {
    if (index !in indices) return this
    val to = (index + offset).coerceIn(indices)
    if (to == index) return this
    return toMutableList().apply { add(to, removeAt(index)) }
}
