package io.github.prithvimk.gymtracker.data.repository

import io.github.prithvimk.gymtracker.data.db.dao.ExerciseHistoryRow
import io.github.prithvimk.gymtracker.data.db.dao.PersonalRecordDao
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutDao
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutExerciseEntry
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutHistoryRow
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutRecord
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

/** What [WorkoutRepository.endIdleWorkout] did with the active workout. */
sealed interface IdleWorkoutResult {
    /** No workout was active, or it was still in use. */
    data object StillActive : IdleWorkoutResult

    /** The workout was finished at [endedAt], its last activity. */
    data class Ended(val workoutId: String, val endedAt: Long) : IdleWorkoutResult

    /** The workout had no sets done, so there was nothing to keep and it was deleted. */
    data object Discarded : IdleWorkoutResult
}

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

    /**
     * Ends the active workout if nothing in it has been touched for [IDLE_LIMIT_MILLIS], so one left
     * running by mistake doesn't count days towards its duration. It ends at its last activity, not
     * now; one with no sets done is deleted instead, since there is nothing in it to keep.
     */
    suspend fun endIdleWorkout(): IdleWorkoutResult {
        val active = workoutDao.getActive() ?: return IdleWorkoutResult.StillActive
        val lastActivity = workoutDao.getLastActivity(active.id) ?: active.startedAt
        if (now() - lastActivity < IDLE_LIMIT_MILLIS) return IdleWorkoutResult.StillActive
        if (workoutDao.countCompletedSets(active.id) == 0) {
            deleteWorkout(active.id)
            return IdleWorkoutResult.Discarded
        }
        workoutDao.finish(active.id, endedAt = lastActivity)
        return IdleWorkoutResult.Ended(active.id, lastActivity)
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

    /** The exercise's sets from the last finished workout that started before [workoutId]. */
    suspend fun setsBefore(exerciseId: String, workoutId: String): List<WorkoutSet> =
        workoutDao.getSetsBefore(exerciseId, workoutId)

    /** Adds a working set to the end of the exercise, prefilled from last session or the set before it. */
    suspend fun addSet(workoutExerciseId: String) {
        workoutDao.appendSet(id = newId(), workoutExerciseId = workoutExerciseId, createdAt = now())
    }

    suspend fun updateSetWeight(setId: String, weightKg: Double) {
        workoutDao.updateSetWeight(setId, weightKg, updatedAt = now())
        recomputeRecordsIfCompleted(setId)
    }

    suspend fun updateSetReps(setId: String, reps: Int) {
        workoutDao.updateSetReps(setId, reps, updatedAt = now())
        recomputeRecordsIfCompleted(setId)
    }

    suspend fun updateSetRpe(setId: String, rpe: Double?) {
        workoutDao.updateSetRpe(setId, rpe, updatedAt = now())
    }

    suspend fun updateSetType(setId: String, type: SetType) {
        workoutDao.updateSetType(setId, type, updatedAt = now())
        recomputeRecordsIfCompleted(setId)
    }

    /**
     * Marks the set complete, or clears the mark. A set in a running workout is done now; one ticked
     * later in a finished workout counts as done when that workout ended, so any record it sets is
     * dated to the session and ordered after the sets done in it.
     */
    suspend fun setSetCompleted(setId: String, completed: Boolean) {
        val time = now()
        val completedAt = if (completed) workoutDao.getWorkoutEndForSet(setId) ?: time else null
        workoutDao.updateSetCompletedAt(setId, completedAt = completedAt, updatedAt = time)
        recomputeRecordsForSet(setId)
    }

    fun observeWorkouts(): Flow<List<Workout>> = workoutDao.observeAll()

    /** Finished workouts for the history list, newest first. */
    fun observeHistory(): Flow<List<WorkoutHistoryRow>> = workoutDao.observeHistory()

    /** Every completed set of the exercise with its workout, newest workout first. */
    fun observeExerciseHistory(exerciseId: String): Flow<List<ExerciseHistoryRow>> =
        workoutDao.observeExerciseHistory(exerciseId)

    fun observeActiveWorkout(): Flow<Workout?> = workoutDao.observeActive()

    fun observeWorkout(id: String): Flow<Workout?> = workoutDao.observeById(id)

    fun observeWorkoutExercises(workoutId: String): Flow<List<WorkoutExercise>> = workoutDao.observeExercises(workoutId)

    fun observeSets(workoutExerciseId: String): Flow<List<WorkoutSet>> = workoutDao.observeSets(workoutExerciseId)

    fun observePersonalRecords(exerciseId: String): Flow<List<PersonalRecord>> =
        personalRecordDao.observeForExercise(exerciseId)

    /** Records set in the workout, with exercise names, in the order the logger shows them. */
    fun observeWorkoutRecords(workoutId: String): Flow<List<WorkoutRecord>> =
        personalRecordDao.observeForWorkout(workoutId)

    /** Saves the workout. Its start time orders sessions, so its exercises' records are worked out again. */
    suspend fun saveWorkout(workout: Workout) {
        workoutDao.upsert(workout.copy(updatedAt = now()))
        recomputeRecords(workoutDao.getExerciseIdsForWorkout(workout.id))
    }

    suspend fun saveWorkoutExercise(workoutExercise: WorkoutExercise) {
        val previousExerciseId = workoutDao.getExercise(workoutExercise.id)?.exerciseId
        workoutDao.upsertExercise(workoutExercise.copy(updatedAt = now()))
        recomputeRecords(listOfNotNull(previousExerciseId, workoutExercise.exerciseId).distinct())
    }

    suspend fun saveSet(set: WorkoutSet) {
        val previousExerciseId = workoutDao.getExerciseIdForSet(set.id)
        workoutDao.upsertSet(set.copy(updatedAt = now()))
        recomputeRecords(listOfNotNull(previousExerciseId, workoutDao.getExerciseIdForSet(set.id)).distinct())
    }

    suspend fun deleteWorkout(id: String) {
        val exerciseIds = workoutDao.getExerciseIdsForWorkout(id)
        workoutDao.deleteById(id)
        recomputeRecords(exerciseIds)
    }

    suspend fun deleteWorkoutExercise(id: String) {
        val exerciseId = workoutDao.getExercise(id)?.exerciseId
        workoutDao.deleteExerciseById(id)
        recomputeRecords(listOfNotNull(exerciseId))
    }

    suspend fun deleteSet(id: String) {
        val exerciseId = workoutDao.getExerciseIdForSet(id)
        workoutDao.deleteSetById(id)
        recomputeRecords(listOfNotNull(exerciseId))
    }

    /**
     * Works out records for every exercise with completed sets if none are stored yet, for sessions
     * logged before PR detection existed. Cheap when there is nothing to do.
     */
    suspend fun backfillPersonalRecordsIfMissing() {
        if (personalRecordDao.count() > 0) return
        recomputeRecords(personalRecordDao.getExerciseIdsWithCompletedSets())
    }

    private suspend fun recomputeRecordsForSet(setId: String) {
        recomputeRecords(listOfNotNull(workoutDao.getExerciseIdForSet(setId)))
    }

    /** Edits to a set not marked complete cannot change any record, and the keypad saves on every key. */
    private suspend fun recomputeRecordsIfCompleted(setId: String) {
        recomputeRecords(listOfNotNull(workoutDao.getExerciseIdForCompletedSet(setId)))
    }

    private suspend fun recomputeRecords(exerciseIds: List<String>) {
        exerciseIds.forEach { personalRecordDao.recompute(it, now = now(), newId = newId) }
    }

    companion object {
        /** How long an active workout can go untouched before [endIdleWorkout] ends it: 3 hours. */
        const val IDLE_LIMIT_MILLIS = 3 * 60 * 60 * 1_000L
    }
}
