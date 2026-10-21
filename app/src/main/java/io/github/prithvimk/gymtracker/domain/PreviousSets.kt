package io.github.prithvimk.gymtracker.domain

import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.model.SetType

/** Weight and reps to start a new set with. */
data class SetPrefill(val weightKg: Double, val reps: Int)

/**
 * Pairs each of [current] with its counterpart in [previous], the same exercise's sets from its last
 * session: the set of the same type in the same place among sets of that type, so warm-ups line up
 * with warm-ups and the second working set with the second working set. Previous sets without reps
 * were never done and are skipped. Sets with no counterpart get null.
 */
fun matchPrevious(current: List<WorkoutSet>, previous: List<WorkoutSet>): List<WorkoutSet?> {
    val previousByType = previous.filter { it.reps > 0 }.sortedBy { it.position }.groupBy { it.type }
    val seen = mutableMapOf<SetType, Int>()
    return current.sortedBy { it.position }.map { set ->
        val index = seen.getOrDefault(set.type, 0)
        seen[set.type] = index + 1
        previousByType[set.type]?.getOrNull(index)
    }
}

/**
 * What a new working set appended after [current] starts with: its counterpart from last session if
 * there is one, otherwise a copy of the last set so far, otherwise blank.
 */
fun prefillForNewSet(current: List<WorkoutSet>, previous: List<WorkoutSet>): SetPrefill {
    val workingSoFar = current.count { it.type == SetType.WORKING }
    val fromLastTime = previous.filter { it.reps > 0 && it.type == SetType.WORKING }
        .sortedBy { it.position }
        .getOrNull(workingSoFar)
    val source = fromLastTime ?: current.maxByOrNull { it.position }
    return source?.let { SetPrefill(it.weightKg, it.reps) } ?: SetPrefill(weightKg = 0.0, reps = 0)
}
