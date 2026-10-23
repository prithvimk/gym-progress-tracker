package io.github.prithvimk.gymtracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Version 2: workout exercises carry the rep range and rest time of the routine they came from. */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_exercises ADD COLUMN repMin INTEGER")
            db.execSQL("ALTER TABLE workout_exercises ADD COLUMN repMax INTEGER")
            db.execSQL("ALTER TABLE workout_exercises ADD COLUMN restSeconds INTEGER")
        }
    }

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
