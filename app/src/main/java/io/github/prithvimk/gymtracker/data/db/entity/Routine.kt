package io.github.prithvimk.gymtracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)
