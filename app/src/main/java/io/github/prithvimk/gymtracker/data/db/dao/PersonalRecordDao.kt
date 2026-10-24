package io.github.prithvimk.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.github.prithvimk.gymtracker.data.db.entity.PersonalRecord
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.ExerciseSession
import io.github.prithvimk.gymtracker.domain.detectPersonalRecords
import kotlinx.coroutines.flow.Flow

/** A completed set of an exercise, with the workout it belongs to. */
data class CompletedSetRow(val workoutId: String, val workoutStartedAt: Long, @Embedded val set: WorkoutSet)

@Dao
interface PersonalRecordDao {
    @Query("SELECT * FROM personal_records WHERE exerciseId = :exerciseId ORDER BY achievedAt DESC")
    fun observeForExercise(exerciseId: String): Flow<List<PersonalRecord>>

    @Query("SELECT * FROM personal_records WHERE exerciseId = :exerciseId")
    suspend fun getForExercise(exerciseId: String): List<PersonalRecord>

    @Query("SELECT COUNT(*) FROM personal_records")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(records: List<PersonalRecord>)

    @Query("DELETE FROM personal_records WHERE exerciseId = :exerciseId")
    suspend fun deleteForExercise(exerciseId: String)

    @Query(
        """
        SELECT w.id AS workoutId, w.startedAt AS workoutStartedAt, s.*
        FROM workout_sets s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND s.completedAt IS NOT NULL
        """
    )
    suspend fun getCompletedSets(exerciseId: String): List<CompletedSetRow>

    /** Exercises with at least one completed set. */
    @Query(
        """
        SELECT DISTINCT we.exerciseId FROM workout_exercises we
        JOIN workout_sets s ON s.workoutExerciseId = we.id
        WHERE s.completedAt IS NOT NULL
        """
    )
    suspend fun getExerciseIdsWithCompletedSets(): List<String>

    /**
     * Works the exercise's records out again from all its completed sets with [detectPersonalRecords]
     * and stores them in place of the old ones. A record that still stands keeps its id and createdAt.
     * One transaction, so recomputes after quick edits never interleave and store a stale result.
     */
    @Transaction
    suspend fun recompute(exerciseId: String, now: Long, newId: () -> String) {
        val sessions =
            getCompletedSets(exerciseId)
                .groupBy { it.workoutId }
                .map { (workoutId, rows) ->
                    ExerciseSession(workoutId, rows.first().workoutStartedAt, rows.map { it.set })
                }
        val existing = getForExercise(exerciseId).associateBy { it.type to it.setId }
        val records =
            detectPersonalRecords(sessions).map { hit ->
                val old = existing[hit.type to hit.setId]
                when {
                    old == null ->
                        PersonalRecord(
                            id = newId(),
                            exerciseId = exerciseId,
                            type = hit.type,
                            value = hit.value,
                            setId = hit.setId,
                            achievedAt = hit.achievedAt,
                            createdAt = now,
                            updatedAt = now,
                        )
                    old.value == hit.value && old.achievedAt == hit.achievedAt -> old
                    else -> old.copy(value = hit.value, achievedAt = hit.achievedAt, updatedAt = now)
                }
            }
        deleteForExercise(exerciseId)
        insertAll(records)
    }
}
