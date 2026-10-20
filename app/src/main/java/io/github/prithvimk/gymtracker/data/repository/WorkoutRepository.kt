package io.github.prithvimk.gymtracker.data.repository

import io.github.prithvimk.gymtracker.data.db.dao.PersonalRecordDao
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutDao
import io.github.prithvimk.gymtracker.data.db.entity.PersonalRecord
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutExercise
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import kotlinx.coroutines.flow.Flow

/** Logged workouts, their exercises and sets, and PRs. Phase 2 builds the logger on top of this. */
class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val personalRecordDao: PersonalRecordDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun observeWorkouts(): Flow<List<Workout>> = workoutDao.observeAll()

    fun observeActiveWorkout(): Flow<Workout?> = workoutDao.observeActive()

    fun observeWorkoutExercises(workoutId: String): Flow<List<WorkoutExercise>> = workoutDao.observeExercises(workoutId)

    fun observeSets(workoutExerciseId: String): Flow<List<WorkoutSet>> = workoutDao.observeSets(workoutExerciseId)

    fun observePersonalRecords(exerciseId: String): Flow<List<PersonalRecord>> {
        return personalRecordDao.observeForExercise(exerciseId)
    }

    suspend fun saveWorkout(workout: Workout) {
        workoutDao.upsert(workout.copy(updatedAt = now()))
    }

    suspend fun saveWorkoutExercise(workoutExercise: WorkoutExercise) {
        workoutDao.upsertExercise(workoutExercise.copy(updatedAt = now()))
    }

    suspend fun saveSet(set: WorkoutSet) {
        workoutDao.upsertSet(set.copy(updatedAt = now()))
    }

    suspend fun deleteWorkout(id: String) {
        workoutDao.deleteById(id)
    }

    suspend fun deleteWorkoutExercise(id: String) {
        workoutDao.deleteExerciseById(id)
    }

    suspend fun deleteSet(id: String) {
        workoutDao.deleteSetById(id)
    }
}
