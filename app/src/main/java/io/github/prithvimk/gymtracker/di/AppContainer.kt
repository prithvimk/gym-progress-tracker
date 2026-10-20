package io.github.prithvimk.gymtracker.di

import android.content.Context

/**
 * Manual dependency container. Repositories and the Room database are added here
 * in Phase 1, so ViewModels can get them without a DI framework.
 */
interface AppContainer

class DefaultAppContainer(
    @Suppress("unused") private val context: Context,
) : AppContainer
