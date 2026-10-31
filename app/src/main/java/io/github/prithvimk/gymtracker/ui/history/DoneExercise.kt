package io.github.prithvimk.gymtracker.ui.history

import io.github.prithvimk.gymtracker.data.db.dao.WorkoutExerciseEntry
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.ui.exercises.HistorySet
import io.github.prithvimk.gymtracker.ui.workout.setLabels

/** An exercise of a past workout as its detail screen shows it: only the sets that were done. */
data class DoneExercise(val entry: WorkoutExerciseEntry, val sets: List<HistorySet>)

/**
 * The workout's [exercises] in order, each with its completed sets from [sets] (keyed by workout
 * exercise id) and the records they hold from [badges]. Sets are numbered among the completed ones,
 * the same as in the exercise's history.
 */
fun doneExercises(
    exercises: List<WorkoutExerciseEntry>,
    sets: Map<String, List<WorkoutSet>>,
    badges: Map<String, List<PrType>>,
): List<DoneExercise> = exercises.map { entry ->
    val done = sets[entry.id].orEmpty().filter { it.completedAt != null }
    val labels = setLabels(done.map { it.type })
    DoneExercise(entry, done.mapIndexed { index, set -> HistorySet(set, labels[index], badges[set.id].orEmpty()) })
}
