package io.github.prithvimk.gymtracker.domain

import io.github.prithvimk.gymtracker.data.db.entity.WorkoutExercise
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.model.SetType

/** Rest between sets when an exercise has no rest time of its own. */
const val DEFAULT_REST_SECONDS = 120

const val MAX_REST_SECONDS = 600
const val REST_STEP_SECONDS = 15
const val MAX_TARGET_SETS = 20
const val MAX_TARGET_REPS = 100
const val DEFAULT_TARGET_SETS = 3
const val DEFAULT_REP_MIN = 8
const val DEFAULT_REP_MAX = 12

/** One exercise's targets in a routine. A rest of 0 means no rest timer. */
data class ExerciseTargets(val targetSets: Int, val repMin: Int, val repMax: Int, val restSeconds: Int) {
    /** Clamped to sensible bounds, with the rep range the right way round and rest on a 15 s step. */
    fun normalized(): ExerciseTargets {
        val low = repMin.coerceIn(1, MAX_TARGET_REPS)
        val high = repMax.coerceIn(1, MAX_TARGET_REPS)
        val rest = restSeconds.coerceIn(0, MAX_REST_SECONDS)
        return ExerciseTargets(
            targetSets = targetSets.coerceIn(1, MAX_TARGET_SETS),
            repMin = minOf(low, high),
            repMax = maxOf(low, high),
            restSeconds = rest - rest % REST_STEP_SECONDS,
        )
    }

    companion object {
        val Default = ExerciseTargets(DEFAULT_TARGET_SETS, DEFAULT_REP_MIN, DEFAULT_REP_MAX, DEFAULT_REST_SECONDS)
    }
}

/** An exercise of a routine being built from a finished workout. */
data class PlannedExercise(val exerciseId: String, val targets: ExerciseTargets)

/**
 * Turns a workout into a routine plan, one entry per workout exercise in order. Warm-ups don't count
 * as target sets. Targets come from the sets that were done, or from all sets if none were; the rep
 * range spans their reps. Rep range and rest the workout got from a routine are kept as they were.
 */
fun planFromWorkout(exercises: List<WorkoutExercise>, sets: List<WorkoutSet>): List<PlannedExercise> {
    val setsByExercise = sets.groupBy { it.workoutExerciseId }
    return exercises.sortedBy { it.position }.map { exercise ->
        val counted = setsByExercise[exercise.id].orEmpty().filter { it.type != SetType.WARMUP }
        val basis = counted.filter { it.completedAt != null }.ifEmpty { counted }
        val reps = basis.map { it.reps }.filter { it > 0 }
        val defaults = ExerciseTargets.Default
        val targets =
            ExerciseTargets(
                targetSets = basis.size.takeIf { it > 0 } ?: defaults.targetSets,
                repMin = exercise.repMin ?: reps.minOrNull() ?: defaults.repMin,
                repMax = exercise.repMax ?: reps.maxOrNull() ?: defaults.repMax,
                restSeconds = exercise.restSeconds ?: defaults.restSeconds,
            )
        PlannedExercise(exercise.exerciseId, targets.normalized())
    }
}
