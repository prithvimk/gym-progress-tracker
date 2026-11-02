package io.github.prithvimk.gymtracker.ui.exercises

import io.github.prithvimk.gymtracker.data.db.dao.ExerciseHistoryRow
import io.github.prithvimk.gymtracker.data.db.entity.PersonalRecord
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.countsForRecords
import io.github.prithvimk.gymtracker.domain.estimatedOneRepMaxKg
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.ui.workout.SetLabel
import io.github.prithvimk.gymtracker.ui.workout.setLabels

/** An exercise's standing record of one type, with the set that holds it. */
data class RecordBest(val type: PrType, val value: Double, val weightKg: Double, val reps: Int, val achievedAt: Long)

/** A completed set in an exercise's history, with the records it holds other than session volume. */
data class HistorySet(val set: WorkoutSet, val label: SetLabel, val records: List<PrType>)

/** An exercise's completed sets in one workout. [volumeRecord] is whether the session holds the volume record. */
data class HistorySession(
    val workoutId: String,
    val startedAt: Long,
    val inProgress: Boolean,
    val routineName: String?,
    val sets: List<HistorySet>,
    val volumeKg: Double,
    val volumeRecord: Boolean,
)

/**
 * The standing record of each type, in [PrType] order. Every record beats the ones before it, so the
 * latest of each type stands. Records whose set isn't in [rows] are left out: the two are read
 * separately, so for a moment after an edit one can be ahead of the other.
 */
fun standingRecords(records: List<PersonalRecord>, rows: List<ExerciseHistoryRow>): List<RecordBest> {
    val setsById = rows.associate { it.set.id to it.set }
    return records
        .filter { it.setId in setsById }
        .groupBy { it.type }
        .map { (type, ofType) ->
            val latest = ofType.maxWith(compareBy({ it.achievedAt }, { it.value }))
            val set = setsById.getValue(latest.setId)
            RecordBest(type, latest.value, set.weightKg, set.reps, latest.achievedAt)
        }.sortedBy { it.type }
}

/** An exercise's best estimated 1RM in one workout. */
data class E1rmPoint(val startedAt: Long, val kg: Double)

/**
 * The best estimated 1RM of each workout in [rows], oldest first. Only the sets that count towards
 * records do, so warm-ups and sets over 12 reps leave a workout out.
 */
fun e1rmPoints(rows: List<ExerciseHistoryRow>): List<E1rmPoint> = rows
    .groupBy { it.workoutId }
    .mapNotNull { (_, sessionRows) ->
        val best = sessionRows
            .filter { countsForRecords(it.set) }
            .mapNotNull { estimatedOneRepMaxKg(it.set.weightKg, it.set.reps) }
            .maxOrNull()
        best?.let { E1rmPoint(sessionRows.first().workoutStartedAt, it) }
    }.sortedBy { it.startedAt }

/**
 * How the latest point compares with the last one at least [days] days before [now], or null if there
 * is no point that old, so the change always spans the whole period.
 */
fun e1rmChange(points: List<E1rmPoint>, now: Long, days: Int = E1RM_CHANGE_DAYS): Double? {
    val latest = points.lastOrNull() ?: return null
    val cutoff = now - days * MILLIS_PER_DAY
    val baseline = points.lastOrNull { it.startedAt <= cutoff } ?: return null
    return if (baseline == latest) null else latest.kg - baseline.kg
}

/** The period the exercise detail's e1RM change covers. */
const val E1RM_CHANGE_DAYS = 30

private const val MILLIS_PER_DAY = 86_400_000L

/** [rows] grouped by workout, keeping their order, with each set's label and records. */
fun historySessions(rows: List<ExerciseHistoryRow>, records: List<PersonalRecord>): List<HistorySession> {
    val typesBySet = records.groupBy({ it.setId }, { it.type })
    return rows.groupBy { it.workoutId }.map { (workoutId, sessionRows) ->
        val first = sessionRows.first()
        val labels = setLabels(sessionRows.map { it.set.type })
        HistorySession(
            workoutId = workoutId,
            startedAt = first.workoutStartedAt,
            inProgress = first.workoutEndedAt == null,
            routineName = first.routineName,
            sets =
            sessionRows.mapIndexed { index, row ->
                val types = typesBySet[row.set.id].orEmpty()
                HistorySet(row.set, labels[index], types.filter { it != PrType.VOLUME }.sorted())
            },
            volumeKg = sessionRows.sumOf { it.set.weightKg * it.set.reps },
            volumeRecord = sessionRows.any { PrType.VOLUME in typesBySet[it.set.id].orEmpty() },
        )
    }
}
