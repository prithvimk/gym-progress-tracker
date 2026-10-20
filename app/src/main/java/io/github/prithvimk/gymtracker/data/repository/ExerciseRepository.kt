package io.github.prithvimk.gymtracker.data.repository

import io.github.prithvimk.gymtracker.data.db.dao.ExerciseDao
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import java.util.UUID
import kotlinx.coroutines.flow.Flow

sealed interface ExerciseSaveResult {
    data class Saved(val id: String) : ExerciseSaveResult

    data object BlankName : ExerciseSaveResult

    data class DuplicateName(val existingName: String) : ExerciseSaveResult

    /** The exercise is missing or built in. Built-in exercises can be archived but keep their details. */
    data object NotEditable : ExerciseSaveResult
}

class ExerciseRepository(
    private val dao: ExerciseDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    /** All exercises, archived ones included, sorted by name. */
    fun observeExercises(): Flow<List<Exercise>> = dao.observeAll()

    fun observeExercise(id: String): Flow<Exercise?> = dao.observeById(id)

    suspend fun createCustom(name: String, muscleGroup: MuscleGroup, equipment: Equipment): ExerciseSaveResult {
        val cleanName = normalizeExerciseName(name)
        validateName(cleanName, excludeId = "")?.let { return it }
        val timestamp = now()
        val exercise =
            Exercise(
                id = newId(),
                name = cleanName,
                muscleGroup = muscleGroup,
                equipment = equipment,
                isCustom = true,
                archived = false,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        dao.insert(exercise)
        return ExerciseSaveResult.Saved(exercise.id)
    }

    suspend fun updateCustom(
        id: String,
        name: String,
        muscleGroup: MuscleGroup,
        equipment: Equipment,
    ): ExerciseSaveResult {
        val existing = dao.getById(id)
        if (existing == null || !existing.isCustom) return ExerciseSaveResult.NotEditable
        val cleanName = normalizeExerciseName(name)
        validateName(cleanName, excludeId = id)?.let { return it }
        val updated =
            existing.copy(
                name = cleanName,
                muscleGroup = muscleGroup,
                equipment = equipment,
                updatedAt = now(),
            )
        dao.update(updated)
        return ExerciseSaveResult.Saved(id)
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        dao.setArchived(id, archived, now())
    }

    private suspend fun validateName(name: String, excludeId: String): ExerciseSaveResult? {
        if (name.isEmpty()) return ExerciseSaveResult.BlankName
        val clash = dao.findByName(name, excludeId) ?: return null
        return ExerciseSaveResult.DuplicateName(clash.name)
    }
}

private val whitespace = Regex("\\s+")

/** Trims the name and collapses runs of whitespace, so "  Cable   Fly " and "Cable Fly" are the same. */
fun normalizeExerciseName(raw: String): String = raw.trim().replace(whitespace, " ")
