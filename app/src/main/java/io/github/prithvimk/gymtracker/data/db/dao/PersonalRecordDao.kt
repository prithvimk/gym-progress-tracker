package io.github.prithvimk.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.prithvimk.gymtracker.data.db.entity.PersonalRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalRecordDao {
    @Query("SELECT * FROM personal_records WHERE exerciseId = :exerciseId ORDER BY achievedAt DESC")
    fun observeForExercise(exerciseId: String): Flow<List<PersonalRecord>>

    @Insert
    suspend fun insertAll(records: List<PersonalRecord>)

    @Query("DELETE FROM personal_records WHERE exerciseId = :exerciseId")
    suspend fun deleteForExercise(exerciseId: String)
}
