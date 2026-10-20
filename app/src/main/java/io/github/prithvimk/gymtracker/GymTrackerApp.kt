package io.github.prithvimk.gymtracker

import android.app.Application
import io.github.prithvimk.gymtracker.di.AppContainer
import io.github.prithvimk.gymtracker.di.DefaultAppContainer

class GymTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
