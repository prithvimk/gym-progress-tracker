package io.github.prithvimk.gymtracker.domain

import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.domain.model.SetType

/** An exercise's sets in one workout. An exercise added twice to a workout is still one session. */
data class ExerciseSession(val workoutId: String, val startedAt: Long, val sets: List<WorkoutSet>)

/** A record set by [setId]: [value] beat every earlier value of its [type] for the exercise. */
data class RecordHit(val type: PrType, val value: Double, val setId: String, val achievedAt: Long)

/** Epley estimated 1RM, or null when the set has no weight or is outside 1 to 12 reps. */
fun estimatedOneRepMaxKg(weightKg: Double, reps: Int): Double? =
    if (weightKg > 0 && reps in 1..MAX_E1RM_REPS) weightKg * (1 + reps / 30.0) else null

/** Only completed working sets with reps count towards records. */
fun countsForRecords(set: WorkoutSet): Boolean = set.type == SetType.WORKING && set.completedAt != null && set.reps > 0

/**
 * Every record in an exercise's history, worked out from scratch so edits and deletes come out right.
 * Sessions are taken in the order they started and sets in the order they were completed.
 *
 * - [PrType.WEIGHT]: heavier than any earlier set.
 * - [PrType.REPS_AT_WEIGHT]: more reps than any earlier set at the same or a heavier weight. A weight
 *   never lifted before has nothing to beat, so it counts only as a weight record.
 * - [PrType.E1RM]: higher estimated 1RM than any earlier set.
 * - [PrType.VOLUME]: more working-set volume than any earlier session, credited to its last set.
 *
 * The first session sets the first records. Within a session, a set that beats an earlier record from
 * the same session takes it over, so each session keeps only its best: one weight record, not one for
 * every heavier set on the way up.
 */
fun detectPersonalRecords(sessions: List<ExerciseSession>): List<RecordHit> {
    val hits = mutableListOf<RecordHit>()
    val earlierSets = mutableListOf<WorkoutSet>()
    var bestWeight = 0.0
    var bestE1rm = 0.0
    var bestVolume = 0.0
    for (session in sessions.sortedWith(compareBy({ it.startedAt }, { it.workoutId }))) {
        val sets = session.sets.filter(::countsForRecords).sortedWith(
            compareBy({
                it.completedAt
            }, { it.position }, { it.id })
        )
        if (sets.isEmpty()) continue
        val sessionHits = mutableListOf<Pair<RecordHit, WorkoutSet>>()
        fun record(type: PrType, value: Double, set: WorkoutSet, supersedes: (WorkoutSet) -> Boolean) {
            sessionHits.removeAll { (hit, hitSet) -> hit.type == type && supersedes(hitSet) }
            sessionHits += RecordHit(type, value, set.id, checkNotNull(set.completedAt)) to set
        }
        for (set in sets) {
            if (set.weightKg > bestWeight) {
                bestWeight = set.weightKg
                record(PrType.WEIGHT, set.weightKg, set) { true }
            }
            val repsToBeat = earlierSets.filter { it.weightKg >= set.weightKg }.maxOfOrNull { it.reps }
            if (repsToBeat != null && set.reps > repsToBeat) {
                record(PrType.REPS_AT_WEIGHT, set.reps.toDouble(), set) {
                    it.weightKg <= set.weightKg &&
                        it.reps < set.reps
                }
            }
            val e1rm = estimatedOneRepMaxKg(set.weightKg, set.reps)
            if (e1rm != null && e1rm > bestE1rm) {
                bestE1rm = e1rm
                record(PrType.E1RM, e1rm, set) { true }
            }
            earlierSets += set
        }
        val volume = sets.sumOf { it.weightKg * it.reps }
        if (volume > bestVolume) {
            bestVolume = volume
            val last = sets.last()
            sessionHits += RecordHit(PrType.VOLUME, volume, last.id, checkNotNull(last.completedAt)) to last
        }
        hits += sessionHits.map { it.first }
    }
    return hits
}

private const val MAX_E1RM_REPS = 12
