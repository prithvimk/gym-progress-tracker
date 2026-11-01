package io.github.prithvimk.gymtracker.domain

/** What [upNext] needs to know about a routine. [lastDoneAt] is null if it has never been done. */
interface RoutineRotation {
    val exerciseCount: Int
    val lastDoneAt: Long?
}

/**
 * The routine to suggest next, from [routines] in list order: one never done comes first, then the one
 * done least recently, and ties go to the earlier in the list. Routines with no exercises are never
 * suggested, since starting one gives an empty workout. Null if no routine qualifies.
 */
fun <T : RoutineRotation> upNext(routines: List<T>): T? = routines
    .filter { it.exerciseCount > 0 }
    .minWithOrNull(compareBy(nullsFirst()) { it.lastDoneAt })
