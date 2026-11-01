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

/** Version 3: an exercise can opt in to the logger's RPE column. Off for every existing exercise. */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE exercises ADD COLUMN tracksRpe INTEGER NOT NULL DEFAULT 0")
        }
    }

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
