package io.github.prithvimk.gymtracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExercise(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseId: String,
    val position: Int,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)
