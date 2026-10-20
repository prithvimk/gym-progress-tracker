package io.github.prithvimk.gymtracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.prithvimk.gymtracker.domain.model.PrType

/** A PR, linked to the set that set it. Deleting that set removes the record so it can be recomputed. */
@Entity(
    tableName = "personal_records",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutSet::class,
            parentColumns = ["id"],
            childColumns = ["setId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId"), Index("setId")],
)
data class PersonalRecord(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val type: PrType,
    val value: Double,
    val setId: String,
    val achievedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
