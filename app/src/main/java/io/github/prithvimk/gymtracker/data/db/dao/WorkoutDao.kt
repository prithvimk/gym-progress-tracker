package io.github.prithvimk.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutExercise
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<Workout>>

    /** The unfinished workout, if any. The app keeps at most one. */
    @Query("SELECT * FROM workouts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: String): Workout?

    @Upsert
    suspend fun upsert(workout: Workout)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    fun observeExercises(workoutId: String): Flow<List<WorkoutExercise>>

    @Upsert
    suspend fun upsertExercise(workoutExercise: WorkoutExercise)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: String)

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY position")
    fun observeSets(workoutExerciseId: String): Flow<List<WorkoutSet>>

    @Upsert
    suspend fun upsertSet(set: WorkoutSet)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSetById(id: String)
}
