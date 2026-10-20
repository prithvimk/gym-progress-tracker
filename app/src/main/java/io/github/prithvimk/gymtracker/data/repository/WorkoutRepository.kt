package io.github.prithvimk.gymtracker.data.repository

import io.github.prithvimk.gymtracker.data.db.dao.PersonalRecordDao
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutDao
import io.github.prithvimk.gymtracker.data.db.entity.PersonalRecord
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutExercise
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/** Logged workouts, their exercises and sets, and PRs. */
class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val personalRecordDao: PersonalRecordDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    /**
     * Starts an empty workout, or returns the active one if there already is one. The app keeps at
     * most one active workout, and because it lives in the database it survives the app being killed.
     */
    suspend fun startEmptyWorkout(): Workout {
        workoutDao.insertEmptyIfNoneActive(id = newId(), startedAt = now())
        return checkNotNull(workoutDao.getActive()) { "No active workout after starting one" }
    }

    /** Ends the workout. Does nothing if it has already finished. */
    suspend fun finishWorkout(id: String) {
        workoutDao.finish(id, endedAt = now())
    }

    fun observeWorkouts(): Flow<List<Workout>> = workoutDao.observeAll()

    fun observeActiveWorkout(): Flow<Workout?> = workoutDao.observeActive()

    fun observeWorkoutExercises(workoutId: String): Flow<List<WorkoutExercise>> = workoutDao.observeExercises(workoutId)

    fun observeSets(workoutExerciseId: String): Flow<List<WorkoutSet>> = workoutDao.observeSets(workoutExerciseId)

    fun observePersonalRecords(exerciseId: String): Flow<List<PersonalRecord>> =
        personalRecordDao.observeForExercise(exerciseId)

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
