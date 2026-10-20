package io.github.prithvimk.gymtracker.ui.exercises

import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup

data class ExerciseFilter(
    val query: String = "",
    val muscleGroup: MuscleGroup? = null,
    val showArchived: Boolean = false,
)

private val whitespace = Regex("\\s+")

/** Every word of the query must appear in the name, in any order, ignoring case. */
fun List<Exercise>.filteredBy(criteria: ExerciseFilter): List<Exercise> {
    val words = criteria.query.trim().split(whitespace).filter { it.isNotEmpty() }
    return filter { exercise ->
        val visible = criteria.showArchived || !exercise.archived
        val inGroup = criteria.muscleGroup == null || exercise.muscleGroup == criteria.muscleGroup
        val matches = words.all { exercise.name.contains(it, ignoreCase = true) }
        visible && inGroup && matches
    }
}
