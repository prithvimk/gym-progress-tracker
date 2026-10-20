package io.github.prithvimk.gymtracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.prithvimk.gymtracker.domain.model.SetType

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutExerciseId")],
)
data class WorkoutSet(
    @PrimaryKey val id: String,
    val workoutExerciseId: String,
    val position: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val type: SetType,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
