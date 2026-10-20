package io.github.prithvimk.gymtracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A logged session. A workout with no [endedAt] is the active one. */
@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("routineId"), Index("startedAt")],
)
data class Workout(
    @PrimaryKey val id: String,
    val routineId: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)
