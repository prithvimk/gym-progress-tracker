package io.github.prithvimk.gymtracker.data.repository

import io.github.prithvimk.gymtracker.data.db.dao.PersonalRecordDao
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutDao
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutExerciseEntry
import io.github.prithvimk.gymtracker.data.db.entity.PersonalRecord
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutExercise
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.WorkoutSummary
import io.github.prithvimk.gymtracker.domain.model.SetType
import io.github.prithvimk.gymtracker.domain.summarizeWorkout
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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

    /**
     * Starts a workout from the routine, with its exercises and target sets already in place and
     * prefilled from last time, or returns the active workout if there already is one.
     */
    suspend fun startWorkoutFromRoutine(routineId: String): Workout {
        workoutDao.insertFromRoutineIfNoneActive(id = newId(), routineId = routineId, startedAt = now(), newId = newId)
        return checkNotNull(workoutDao.getActive()) { "No active workout after starting one" }
    }

    fun observeRoutineName(workoutId: String): Flow<String?> = workoutDao.observeRoutineName(workoutId)

    /** Ends the workout. Does nothing if it has already finished. */
    suspend fun finishWorkout(id: String) {
        workoutDao.finish(id, endedAt = now())
    }

    /** Duration, completed sets and volume of the workout, or null if it does not exist. */
    suspend fun summarize(id: String): WorkoutSummary? {
        val workout = workoutDao.getById(id) ?: return null
        return summarizeWorkout(workout, workoutDao.observeSetsForWorkout(id).first(), now = now())
    }

    suspend fun updateWorkoutNotes(id: String, notes: String) {
        workoutDao.updateNotes(id, notes, updatedAt = now())
    }

    fun observeExerciseEntries(workoutId: String): Flow<List<WorkoutExerciseEntry>> =
        workoutDao.observeExerciseEntries(workoutId)

    /** Adds the exercises to the end of the workout, in the order given. The same exercise may appear twice. */
    suspend fun addExercises(workoutId: String, exerciseIds: List<String>) {
        exerciseIds.forEach { exerciseId ->
            workoutDao.appendExercise(id = newId(), workoutId = workoutId, exerciseId = exerciseId, createdAt = now())
        }
    }

    /** Moves an exercise [offset] places within its workout; negative moves it up. */
    suspend fun moveExercise(workoutId: String, workoutExerciseId: String, offset: Int) {
        workoutDao.moveExercise(workoutId, workoutExerciseId, offset, updatedAt = now())
    }

    suspend fun updateExerciseNotes(workoutExerciseId: String, notes: String) {
        workoutDao.updateExerciseNotes(workoutExerciseId, notes, updatedAt = now())
    }

    /** Sets the exercise's rest between sets for this workout; 0 turns the rest timer off. */
    suspend fun updateExerciseRest(workoutExerciseId: String, restSeconds: Int) {
        workoutDao.updateExerciseRest(workoutExerciseId, restSeconds, updatedAt = now())
    }

    fun observeSetsForWorkout(workoutId: String): Flow<List<WorkoutSet>> = workoutDao.observeSetsForWorkout(workoutId)

    /** The exercise's sets from its most recent finished workout other than [currentWorkoutId]. */
    suspend fun previousSets(exerciseId: String, currentWorkoutId: String): List<WorkoutSet> =
        workoutDao.getPreviousSets(exerciseId, excludingWorkoutId = currentWorkoutId)

    /** Adds a working set to the end of the exercise, prefilled from last session or the set before it. */
    suspend fun addSet(workoutExerciseId: String) {
        workoutDao.appendSet(id = newId(), workoutExerciseId = workoutExerciseId, createdAt = now())
    }

    suspend fun updateSetWeight(setId: String, weightKg: Double) {
        workoutDao.updateSetWeight(setId, weightKg, updatedAt = now())
    }

    suspend fun updateSetReps(setId: String, reps: Int) {
        workoutDao.updateSetReps(setId, reps, updatedAt = now())
    }

    suspend fun updateSetRpe(setId: String, rpe: Double?) {
        workoutDao.updateSetRpe(setId, rpe, updatedAt = now())
    }

    suspend fun updateSetType(setId: String, type: SetType) {
        workoutDao.updateSetType(setId, type, updatedAt = now())
    }

    /** Marks the set complete now, or clears the mark. */
    suspend fun setSetCompleted(setId: String, completed: Boolean) {
        val time = now()
        workoutDao.updateSetCompletedAt(setId, completedAt = if (completed) time else null, updatedAt = time)
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
