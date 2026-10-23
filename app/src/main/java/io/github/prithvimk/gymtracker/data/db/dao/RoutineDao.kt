package io.github.prithvimk.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.prithvimk.gymtracker.data.db.entity.Routine
import io.github.prithvimk.gymtracker.data.db.entity.RoutineExercise
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import kotlinx.coroutines.flow.Flow

/** An exercise in a routine, joined with the library entry it points at so the editor can show its name. */
data class RoutineExerciseEntry(
    val id: String,
    val exerciseId: String,
    val position: Int,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val restSeconds: Int,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
)

/** The name of one exercise in a routine, for the routine list. */
data class RoutineExerciseName(val routineId: String, val name: String)

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Routine>>

    /** Exercise names of every routine, each routine's in order. */
    @Query(
        """
        SELECT re.routineId, e.name FROM routine_exercises re JOIN exercises e ON e.id = re.exerciseId
        ORDER BY re.routineId, re.position
        """
    )
    fun observeExerciseNames(): Flow<List<RoutineExerciseName>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getById(id: String): Routine?

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY position")
    fun observeExercises(routineId: String): Flow<List<RoutineExercise>>

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY position")
    suspend fun getExercises(routineId: String): List<RoutineExercise>

    @Query(
        """
        SELECT re.id, re.exerciseId, re.position, re.targetSets, re.repMin, re.repMax, re.restSeconds,
            e.name, e.muscleGroup, e.equipment
        FROM routine_exercises re JOIN exercises e ON e.id = re.exerciseId
        WHERE re.routineId = :routineId
        ORDER BY re.position
        """
    )
    suspend fun getExerciseEntries(routineId: String): List<RoutineExerciseEntry>

    @Upsert
    suspend fun upsert(routine: Routine)

    @Upsert
    suspend fun upsertExercise(routineExercise: RoutineExercise)

    @Upsert
    suspend fun upsertExercises(routineExercises: List<RoutineExercise>)

    /**
     * Saves a routine and replaces its exercises with [exercises], in one transaction so the list
     * never shows half an edit. Exercises that are no longer listed are deleted.
     */
    @Transaction
    suspend fun replace(routine: Routine, exercises: List<RoutineExercise>) {
        upsert(routine)
        deleteExercisesExcept(routine.id, exercises.map { it.id })
        upsertExercises(exercises)
    }

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId AND id NOT IN (:keepIds)")
    suspend fun deleteExercisesExcept(routineId: String, keepIds: List<String>)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM routine_exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: String)
}
