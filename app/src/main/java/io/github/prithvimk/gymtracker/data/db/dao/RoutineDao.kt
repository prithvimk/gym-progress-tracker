package io.github.prithvimk.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.prithvimk.gymtracker.data.db.entity.Routine
import io.github.prithvimk.gymtracker.data.db.entity.RoutineExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getById(id: String): Routine?

    @Query("SELECT * FROM routine_exercises WHERE routineId = :routineId ORDER BY position")
    fun observeExercises(routineId: String): Flow<List<RoutineExercise>>

    @Upsert
    suspend fun upsert(routine: Routine)

    @Upsert
    suspend fun upsertExercise(routineExercise: RoutineExercise)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM routine_exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: String)
}
