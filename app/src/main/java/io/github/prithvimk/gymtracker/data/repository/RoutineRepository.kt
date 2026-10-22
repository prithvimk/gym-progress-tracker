package io.github.prithvimk.gymtracker.data.repository

import io.github.prithvimk.gymtracker.data.db.dao.RoutineDao
import io.github.prithvimk.gymtracker.data.db.dao.RoutineExerciseEntry
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutDao
import io.github.prithvimk.gymtracker.data.db.entity.Routine
import io.github.prithvimk.gymtracker.data.db.entity.RoutineExercise
import io.github.prithvimk.gymtracker.domain.ExerciseTargets
import io.github.prithvimk.gymtracker.domain.planFromWorkout
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** A routine with the names of its exercises in order, for the routine list. */
data class RoutineSummary(val routine: Routine, val exerciseNames: List<String>)

/** A routine as the editor loads it. */
data class RoutineDetail(val routine: Routine, val exercises: List<RoutineExerciseEntry>)

/** One exercise as the editor saves it. [id] is null for an exercise added in this edit. */
data class RoutineExerciseInput(val id: String?, val exerciseId: String, val targets: ExerciseTargets)

sealed interface RoutineSaveResult {
    data class Saved(val id: String) : RoutineSaveResult

    data object BlankName : RoutineSaveResult
}

/** Routines and their planned exercises. */
class RoutineRepository(
    private val dao: RoutineDao,
    private val workoutDao: WorkoutDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    fun observeRoutines(): Flow<List<Routine>> = dao.observeAll()

    /** Every routine, sorted by name, with its exercise names. */
    fun observeRoutineSummaries(): Flow<List<RoutineSummary>> =
        combine(dao.observeAll(), dao.observeExerciseNames()) { routines, names ->
            val byRoutine = names.groupBy({ it.routineId }, { it.name })
            routines.map { RoutineSummary(it, byRoutine[it.id].orEmpty()) }
        }

    fun observeRoutineExercises(routineId: String): Flow<List<RoutineExercise>> = dao.observeExercises(routineId)

    suspend fun getRoutineDetail(id: String): RoutineDetail? {
        val routine = dao.getById(id) ?: return null
        return RoutineDetail(routine, dao.getExerciseEntries(id))
    }

    /**
     * Creates a routine, or replaces routine [id]'s name, notes and exercises, in the order given.
     * Targets are normalized before they are stored.
     */
    suspend fun saveRoutine(
        id: String?,
        name: String,
        notes: String,
        exercises: List<RoutineExerciseInput>,
    ): RoutineSaveResult {
        val cleanName = normalizeExerciseName(name)
        if (cleanName.isEmpty()) return RoutineSaveResult.BlankName
        val timestamp = now()
        val existing = id?.let { dao.getById(it) }
        val routine =
            existing?.copy(name = cleanName, notes = notes.trim(), updatedAt = timestamp)
                ?: Routine(
                    id = id ?: newId(),
                    name = cleanName,
                    notes = notes.trim(),
                    createdAt = timestamp,
                    updatedAt = timestamp,
                )
        val previous = if (existing == null) emptyMap() else dao.getExercises(routine.id).associateBy { it.id }
        val rows =
            exercises.mapIndexed { index, input ->
                val targets = input.targets.normalized()
                val rowId = input.id?.takeIf { it in previous } ?: newId()
                RoutineExercise(
                    id = rowId,
                    routineId = routine.id,
                    exerciseId = input.exerciseId,
                    position = index,
                    targetSets = targets.targetSets,
                    repMin = targets.repMin,
                    repMax = targets.repMax,
                    restSeconds = targets.restSeconds,
                    createdAt = previous[rowId]?.createdAt ?: timestamp,
                    updatedAt = timestamp,
                )
            }
        dao.replace(routine, rows)
        return RoutineSaveResult.Saved(routine.id)
    }

    /** Saves a finished workout's exercises as a new routine, with targets taken from what was logged. */
    suspend fun createFromWorkout(workoutId: String, name: String): RoutineSaveResult {
        val exercises = workoutDao.getExercises(workoutId)
        val sets = workoutDao.observeSetsForWorkout(workoutId).first()
        val plan = planFromWorkout(exercises, sets)
        return saveRoutine(
            id = null,
            name = name,
            notes = "",
            exercises = plan.map { RoutineExerciseInput(id = null, exerciseId = it.exerciseId, targets = it.targets) },
        )
    }

    /** Deletes the routine. Workouts started from it are kept and simply lose the link. */
    suspend fun deleteRoutine(id: String) {
        dao.deleteById(id)
    }
}
