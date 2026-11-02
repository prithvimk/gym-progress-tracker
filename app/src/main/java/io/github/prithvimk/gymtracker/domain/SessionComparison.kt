package io.github.prithvimk.gymtracker.domain

import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.model.SetType

/** An exercise's best set of a session: the heaviest, and of those the most reps. */
data class TopSet(val weightKg: Double, val reps: Int)

/**
 * How a top set compares with the one before it. [BETTER] and [WORSE] are only for clear cases, where
 * weight and reps both moved the same way or one held; [MIXED] is more weight for fewer reps, or the
 * other way round, which the numbers alone don't settle.
 */
enum class TopSetChange { BETTER, SAME, WORSE, MIXED }

/** One exercise of a session with its sets, as [compareSessions] takes it. */
data class SessionExercise(val exerciseId: String, val name: String, val sets: List<WorkoutSet>)

/** An exercise done in both sessions, with its top set in each. */
data class ExerciseComparison(
    val exerciseId: String,
    val name: String,
    val topSet: TopSet,
    val previousTopSet: TopSet,
) {
    val change: TopSetChange
        get() {
            val weight = topSet.weightKg.compareTo(previousTopSet.weightKg)
            val reps = topSet.reps.compareTo(previousTopSet.reps)
            return when {
                weight == 0 && reps == 0 -> TopSetChange.SAME
                weight >= 0 && reps >= 0 -> TopSetChange.BETTER
                weight <= 0 && reps <= 0 -> TopSetChange.WORSE
                else -> TopSetChange.MIXED
            }
        }
}

/** A session against the previous one: both volumes, and each exercise done in both. */
data class SessionComparison(
    val volumeKg: Double,
    val previousVolumeKg: Double,
    val exercises: List<ExerciseComparison>,
) {
    /** The change in volume as a fraction of the previous one, e.g. 0.039; null if there was none before. */
    val volumeChange: Double?
        get() = if (previousVolumeKg > 0) (volumeKg - previousVolumeKg) / previousVolumeKg else null

    /** Whether there is anything to show. */
    val isEmpty: Boolean get() = volumeChange == null && exercises.isEmpty()
}

/**
 * The top set among the completed sets that aren't warm-ups: the heaviest, and of the heaviest the
 * one with the most reps. Null if there is no such set.
 */
fun topSet(sets: List<WorkoutSet>): TopSet? = bestOf(
    sets.filter { it.completedAt != null && it.type != SetType.WARMUP }.map { TopSet(it.weightKg, it.reps) },
)

/** The heaviest of [sets], and of the heaviest the one with the most reps. */
fun bestOf(sets: List<TopSet>): TopSet? = sets.maxWithOrNull(compareBy<TopSet> { it.weightKg }.thenBy { it.reps })

/** An exercise's top set from the last finished workout it was done in, and when that workout started. */
data class LastTopSet(val topSet: TopSet, val startedAt: Long)

/**
 * Compares a session with the previous one. Exercises are matched by library exercise, an exercise
 * done twice in a session counts all its sets, and only exercises with a top set in both sessions are
 * listed, in the order [current] first has them.
 */
fun compareSessions(current: List<SessionExercise>, previous: List<SessionExercise>): SessionComparison {
    val previousTops =
        previous.groupBy { it.exerciseId }.mapValues { (_, appearances) -> topSet(appearances.allSets()) }
    val exercises =
        current.groupBy { it.exerciseId }.mapNotNull { (exerciseId, appearances) ->
            val top = topSet(appearances.allSets()) ?: return@mapNotNull null
            val previousTop = previousTops[exerciseId] ?: return@mapNotNull null
            ExerciseComparison(exerciseId, appearances.first().name, top, previousTop)
        }
    return SessionComparison(
        volumeKg = volumeKg(current.allSets()),
        previousVolumeKg = volumeKg(previous.allSets()),
        exercises = exercises,
    )
}

private fun List<SessionExercise>.allSets(): List<WorkoutSet> = flatMap { it.sets }
