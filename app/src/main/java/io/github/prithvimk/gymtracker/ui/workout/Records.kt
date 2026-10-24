package io.github.prithvimk.gymtracker.ui.workout

import io.github.prithvimk.gymtracker.data.db.dao.WorkoutRecord
import io.github.prithvimk.gymtracker.domain.model.PrType

/** One exercise's records from a workout, for the finish summary. */
data class ExerciseRecords(val exerciseName: String, val records: List<WorkoutRecord>)

/**
 * The record types each set holds, keyed by set id, for the badge in the logger. Session volume is
 * left out: it belongs to the whole session, and would hop from set to set as each one is ticked.
 */
fun setBadges(records: List<WorkoutRecord>): Map<String, List<PrType>> = records
    .filter { it.type != PrType.VOLUME }
    .groupBy({ it.setId }, { it.type })
    .mapValues { (_, types) -> types.sorted() }

/** Records grouped by exercise, in the order they come, each exercise's in [PrType] order. */
fun recordsByExercise(records: List<WorkoutRecord>): List<ExerciseRecords> = records
    .groupBy { it.exerciseId }
    .map { (_, forExercise) -> ExerciseRecords(forExercise.first().exerciseName, forExercise.sortedBy { it.type }) }
