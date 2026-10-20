package io.github.prithvimk.gymtracker.di

import android.content.Context
import io.github.prithvimk.gymtracker.data.db.GymDatabase
import io.github.prithvimk.gymtracker.data.repository.ExerciseRepository
import io.github.prithvimk.gymtracker.data.repository.RoutineRepository
import io.github.prithvimk.gymtracker.data.repository.WorkoutRepository
import io.github.prithvimk.gymtracker.data.seed.ExerciseSeeder
import io.github.prithvimk.gymtracker.data.seed.SharedPreferencesSeedVersionStore

/** Manual dependency container. ViewModels reach repositories through here without a DI framework. */
interface AppContainer {
    val exerciseRepository: ExerciseRepository
    val routineRepository: RoutineRepository
    val workoutRepository: WorkoutRepository
    val exerciseSeeder: ExerciseSeeder
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    private val database: GymDatabase by lazy { GymDatabase.create(appContext) }

    override val exerciseRepository: ExerciseRepository by lazy { ExerciseRepository(database.exerciseDao()) }

    override val routineRepository: RoutineRepository by lazy { RoutineRepository(database.routineDao()) }

    override val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutDao(), database.personalRecordDao())
    }

    override val exerciseSeeder: ExerciseSeeder by lazy {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ExerciseSeeder(
            exerciseDao = database.exerciseDao(),
            versionStore = SharedPreferencesSeedVersionStore(prefs),
            loadSeedJson = ::readSeedAsset,
        )
    }

    private fun readSeedAsset(): String {
        val stream = appContext.assets.open(ExerciseSeeder.ASSET_PATH)
        return stream.bufferedReader().use { it.readText() }
    }

    private companion object {
        const val PREFS_NAME = "gym_tracker"
    }
}
