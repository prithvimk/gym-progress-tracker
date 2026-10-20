package io.github.prithvimk.gymtracker.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup

/** An exercise in the library. Exercises are archived, never deleted, so history keeps its references. */
@Entity(
    tableName = "exercises",
    indices = [Index("name")],
)
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val isCustom: Boolean,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
