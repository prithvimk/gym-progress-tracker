package io.github.prithvimk.gymtracker.domain

import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet

/** What a finished workout amounts to. Only completed sets count. */
data class WorkoutSummary(val durationMillis: Long, val exerciseCount: Int, val setCount: Int, val volumeKg: Double)

/** Total weight moved: weight × reps over the completed sets, warm-ups included. Bodyweight sets add nothing. */
fun volumeKg(sets: List<WorkoutSet>): Double = sets.filter { it.completedAt != null }.sumOf { it.weightKg * it.reps }

/**
 * Sums up [workout] from its [sets]. Duration runs from start to end, or to [now] if it has not
 * ended; exercises count only if they have a completed set.
 */
fun summarizeWorkout(workout: Workout, sets: List<WorkoutSet>, now: Long): WorkoutSummary {
    val completed = sets.filter { it.completedAt != null }
    val end = workout.endedAt ?: now
    return WorkoutSummary(
        durationMillis = (end - workout.startedAt).coerceAtLeast(0L),
        exerciseCount = completed.map { it.workoutExerciseId }.distinct().size,
        setCount = completed.size,
        volumeKg = volumeKg(completed),
    )
}
