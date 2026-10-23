package io.github.prithvimk.gymtracker.di

import android.content.Context
import io.github.prithvimk.gymtracker.data.db.GymDatabase
import io.github.prithvimk.gymtracker.data.repository.ExerciseRepository
import io.github.prithvimk.gymtracker.data.repository.RoutineRepository
import io.github.prithvimk.gymtracker.data.repository.WorkoutRepository
import io.github.prithvimk.gymtracker.data.seed.ExerciseSeeder
import io.github.prithvimk.gymtracker.data.seed.SharedPreferencesSeedVersionStore
import io.github.prithvimk.gymtracker.timer.RestTimer
import io.github.prithvimk.gymtracker.timer.RestTimerService
import io.github.prithvimk.gymtracker.timer.SharedPreferencesRestTimerStore

/** Manual dependency container. ViewModels reach repositories through here without a DI framework. */
interface AppContainer {
    val exerciseRepository: ExerciseRepository
    val routineRepository: RoutineRepository
    val workoutRepository: WorkoutRepository
    val exerciseSeeder: ExerciseSeeder
    val restTimer: RestTimer
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    private val database: GymDatabase by lazy { GymDatabase.create(appContext) }

    override val exerciseRepository: ExerciseRepository by lazy { ExerciseRepository(database.exerciseDao()) }

    override val routineRepository: RoutineRepository by lazy {
        RoutineRepository(database.routineDao(), database.workoutDao())
    }

    override val workoutRepository: WorkoutRepository by lazy {
        WorkoutRepository(database.workoutDao(), database.personalRecordDao())
    }

    private val prefs by lazy { appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    override val exerciseSeeder: ExerciseSeeder by lazy {
        ExerciseSeeder(
            exerciseDao = database.exerciseDao(),
            versionStore = SharedPreferencesSeedVersionStore(prefs),
            loadSeedJson = ::readSeedAsset,
        )
    }

    override val restTimer: RestTimer by lazy {
        RestTimer(SharedPreferencesRestTimerStore(prefs), onRun = { RestTimerService.start(appContext) })
    }

    private fun readSeedAsset(): String {
        val stream = appContext.assets.open(ExerciseSeeder.ASSET_PATH)
        return stream.bufferedReader().use { it.readText() }
    }

    private companion object {
        const val PREFS_NAME = "gym_tracker"
    }
}
