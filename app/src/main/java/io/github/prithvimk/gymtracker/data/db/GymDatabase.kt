package io.github.prithvimk.gymtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.prithvimk.gymtracker.data.db.dao.ExerciseDao
import io.github.prithvimk.gymtracker.data.db.dao.PersonalRecordDao
import io.github.prithvimk.gymtracker.data.db.dao.RoutineDao
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutDao
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.data.db.entity.PersonalRecord
import io.github.prithvimk.gymtracker.data.db.entity.Routine
import io.github.prithvimk.gymtracker.data.db.entity.RoutineExercise
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutExercise
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet

/**
 * The app database. Every schema change bumps [version], ships a migration and a migration test,
 * and commits the exported schema in `app/schemas`.
 */
@Database(
    entities = [
        Exercise::class,
        Routine::class,
        RoutineExercise::class,
        Workout::class,
        WorkoutExercise::class,
        WorkoutSet::class,
        PersonalRecord::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao

    abstract fun routineDao(): RoutineDao

    abstract fun workoutDao(): WorkoutDao

    abstract fun personalRecordDao(): PersonalRecordDao

    companion object {
        const val NAME = "gym-tracker.db"

        fun create(context: Context): GymDatabase {
            return Room.databaseBuilder(context, GymDatabase::class.java, NAME).withDefaults().build()
        }
    }
}

/** Settings shared by the app database and test databases. */
fun RoomDatabase.Builder<GymDatabase>.withDefaults(): RoomDatabase.Builder<GymDatabase> = addCallback(EnableForeignKeys)

private object EnableForeignKeys : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = ON")
    }
}
