package io.github.prithvimk.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutExercise
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.domain.model.SetType
import io.github.prithvimk.gymtracker.domain.prefillForNewSet
import kotlinx.coroutines.flow.Flow

/** An exercise in a workout, joined with the library entry it points at so the logger can show its name. */
data class WorkoutExerciseEntry(
    val id: String,
    val exerciseId: String,
    val position: Int,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val notes: String,
)

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<Workout>>

    /** The unfinished workout, if any. The app keeps at most one. */
    @Query("SELECT * FROM workouts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActive(): Workout?

    /**
     * Inserts an empty workout unless one is already active, as a single statement so two quick
     * taps can never leave two active workouts.
     */
    @Query(
        """
        INSERT INTO workouts (id, routineId, startedAt, endedAt, notes, createdAt, updatedAt)
        SELECT :id, NULL, :startedAt, NULL, '', :startedAt, :startedAt
        WHERE NOT EXISTS (SELECT 1 FROM workouts WHERE endedAt IS NULL)
        """
    )
    suspend fun insertEmptyIfNoneActive(id: String, startedAt: Long)

    @Query("UPDATE workouts SET endedAt = :endedAt, updatedAt = :endedAt WHERE id = :id AND endedAt IS NULL")
    suspend fun finish(id: String, endedAt: Long)

    @Query("UPDATE workouts SET notes = :notes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String, updatedAt: Long)

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getById(id: String): Workout?

    @Upsert
    suspend fun upsert(workout: Workout)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    fun observeExercises(workoutId: String): Flow<List<WorkoutExercise>>

    @Query(
        """
        SELECT we.id, we.exerciseId, we.position, e.name, e.muscleGroup, e.equipment, we.notes
        FROM workout_exercises we JOIN exercises e ON e.id = we.exerciseId
        WHERE we.workoutId = :workoutId
        ORDER BY we.position
        """
    )
    fun observeExerciseEntries(workoutId: String): Flow<List<WorkoutExerciseEntry>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    suspend fun getExercises(workoutId: String): List<WorkoutExercise>

    /** Appends an exercise after the workout's last one, in one statement so quick adds never share a position. */
    @Query(
        """
        INSERT INTO workout_exercises (id, workoutId, exerciseId, position, notes, createdAt, updatedAt)
        SELECT :id, :workoutId, :exerciseId, COALESCE(MAX(position) + 1, 0), '', :createdAt, :createdAt
        FROM workout_exercises WHERE workoutId = :workoutId
        """
    )
    suspend fun appendExercise(id: String, workoutId: String, exerciseId: String, createdAt: Long)

    /**
     * Moves an exercise [offset] places (negative is up), clamped to the list, and renumbers positions
     * from 0 so gaps left by removed exercises close up.
     */
    @Transaction
    suspend fun moveExercise(workoutId: String, workoutExerciseId: String, offset: Int, updatedAt: Long) {
        val ordered = getExercises(workoutId).toMutableList()
        val from = ordered.indexOfFirst { it.id == workoutExerciseId }
        if (from < 0) return
        val to = (from + offset).coerceIn(ordered.indices)
        if (to == from) return
        ordered.add(to, ordered.removeAt(from))
        val changed =
            ordered.mapIndexedNotNull { index, exercise ->
                if (exercise.position == index) null else exercise.copy(position = index, updatedAt = updatedAt)
            }
        upsertExercises(changed)
    }

    @Upsert
    suspend fun upsertExercise(workoutExercise: WorkoutExercise)

    @Upsert
    suspend fun upsertExercises(workoutExercises: List<WorkoutExercise>)

    @Query("UPDATE workout_exercises SET notes = :notes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateExerciseNotes(id: String, notes: String, updatedAt: Long)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: String)

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY position")
    fun observeSets(workoutExerciseId: String): Flow<List<WorkoutSet>>

    /** Every set in the workout, in exercise order and then set order. */
    @Query(
        """
        SELECT s.* FROM workout_sets s JOIN workout_exercises we ON we.id = s.workoutExerciseId
        WHERE we.workoutId = :workoutId
        ORDER BY we.position, s.position
        """
    )
    fun observeSetsForWorkout(workoutId: String): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getExercise(id: String): WorkoutExercise?

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY position")
    suspend fun getSets(workoutExerciseId: String): List<WorkoutSet>

    /**
     * The sets an exercise had in its most recent finished workout other than [excludingWorkoutId].
     * If it appeared twice in that workout, the first appearance counts.
     */
    @Query(
        """
        SELECT * FROM workout_sets WHERE workoutExerciseId = (
            SELECT we.id FROM workout_exercises we JOIN workouts w ON w.id = we.workoutId
            WHERE we.exerciseId = :exerciseId AND w.endedAt IS NOT NULL AND w.id != :excludingWorkoutId
            ORDER BY w.startedAt DESC, we.position
            LIMIT 1
        )
        ORDER BY position
        """
    )
    suspend fun getPreviousSets(exerciseId: String, excludingWorkoutId: String): List<WorkoutSet>

    /**
     * Appends a working set after the exercise's last one, prefilled by [prefillForNewSet]. A transaction,
     * so quick taps never share a position.
     */
    @Transaction
    suspend fun appendSet(id: String, workoutExerciseId: String, createdAt: Long) {
        val exercise = getExercise(workoutExerciseId) ?: return
        val current = getSets(workoutExerciseId)
        val prefill = prefillForNewSet(current, getPreviousSets(exercise.exerciseId, exercise.workoutId))
        upsertSet(
            WorkoutSet(
                id = id,
                workoutExerciseId = workoutExerciseId,
                position = (current.maxOfOrNull { it.position } ?: -1) + 1,
                weightKg = prefill.weightKg,
                reps = prefill.reps,
                rpe = null,
                type = SetType.WORKING,
                completedAt = null,
                createdAt = createdAt,
                updatedAt = createdAt,
            ),
        )
    }

    // Each field has its own update so edits to different fields of a set never overwrite each other.
    @Query("UPDATE workout_sets SET weightKg = :weightKg, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSetWeight(id: String, weightKg: Double, updatedAt: Long)

    @Query("UPDATE workout_sets SET reps = :reps, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSetReps(id: String, reps: Int, updatedAt: Long)

    @Query("UPDATE workout_sets SET rpe = :rpe, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSetRpe(id: String, rpe: Double?, updatedAt: Long)

    @Query("UPDATE workout_sets SET type = :type, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSetType(id: String, type: SetType, updatedAt: Long)

    @Query("UPDATE workout_sets SET completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSetCompletedAt(id: String, completedAt: Long?, updatedAt: Long)

    @Upsert
    suspend fun upsertSet(set: WorkoutSet)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSetById(id: String)
}
