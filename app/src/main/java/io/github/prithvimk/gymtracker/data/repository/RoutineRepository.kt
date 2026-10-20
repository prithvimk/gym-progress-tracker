package io.github.prithvimk.gymtracker.data.repository

import io.github.prithvimk.gymtracker.data.db.dao.RoutineDao
import io.github.prithvimk.gymtracker.data.db.entity.Routine
import io.github.prithvimk.gymtracker.data.db.entity.RoutineExercise
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Routines and their planned exercises. Phase 3 builds the editor on top of this. */
class RoutineRepository(
    private val dao: RoutineDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    fun observeRoutines(): Flow<List<Routine>> = dao.observeAll()

    fun observeRoutineExercises(routineId: String): Flow<List<RoutineExercise>> = dao.observeExercises(routineId)

    suspend fun createRoutine(name: String, notes: String): String {
        val timestamp = now()
        val routine =
            Routine(
                id = newId(),
                name = name.trim(),
                notes = notes,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
        dao.upsert(routine)
        return routine.id
    }

    suspend fun saveRoutine(routine: Routine) {
        dao.upsert(routine.copy(updatedAt = now()))
    }

    suspend fun saveRoutineExercise(routineExercise: RoutineExercise) {
        dao.upsertExercise(routineExercise.copy(updatedAt = now()))
    }

    suspend fun deleteRoutine(id: String) {
        dao.deleteById(id)
    }

    suspend fun deleteRoutineExercise(id: String) {
        dao.deleteExerciseById(id)
    }
}
