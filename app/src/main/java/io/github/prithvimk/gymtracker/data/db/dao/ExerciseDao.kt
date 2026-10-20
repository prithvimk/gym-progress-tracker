package io.github.prithvimk.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeById(id: String): Flow<Exercise?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): Exercise?

    /** Case-insensitive name lookup, skipping the exercise with [excludeId] (pass "" to skip none). */
    @Query("SELECT * FROM exercises WHERE name = :name COLLATE NOCASE AND id != :excludeId LIMIT 1")
    suspend fun findByName(name: String, excludeId: String): Exercise?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(exercise: Exercise)

    /** Inserts rows whose id is new and leaves existing rows untouched. Returns -1 for each skipped row. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringExisting(exercises: List<Exercise>): List<Long>

    @Update
    suspend fun update(exercise: Exercise)

    @Query("UPDATE exercises SET archived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)
}
