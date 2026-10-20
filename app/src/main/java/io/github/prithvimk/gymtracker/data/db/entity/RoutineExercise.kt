package io.github.prithvimk.gymtracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One planned exercise inside a routine, with its targets. */
@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("routineId"), Index("exerciseId")],
)
data class RoutineExercise(
    @PrimaryKey val id: String,
    val routineId: String,
    val exerciseId: String,
    val position: Int,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val restSeconds: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
