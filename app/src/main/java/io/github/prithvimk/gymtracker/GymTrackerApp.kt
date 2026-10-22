package io.github.prithvimk.gymtracker

import android.app.Application
import android.util.Log
import io.github.prithvimk.gymtracker.di.AppContainer
import io.github.prithvimk.gymtracker.di.DefaultAppContainer
import io.github.prithvimk.gymtracker.timer.RestTimer
import io.github.prithvimk.gymtracker.timer.RestTimerOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymTrackerApp :
    Application(),
    RestTimerOwner {
    lateinit var container: AppContainer
        private set

    override val restTimer: RestTimer get() = container.restTimer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        applicationScope.launch {
            runCatching { container.exerciseSeeder.seedIfNeeded() }
                .onFailure { Log.e(TAG, "Seeding the exercise library failed", it) }
        }
    }

    private companion object {
        const val TAG = "GymTrackerApp"
    }
}
