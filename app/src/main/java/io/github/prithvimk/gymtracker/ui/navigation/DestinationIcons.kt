package io.github.prithvimk.gymtracker.ui.navigation

import androidx.annotation.DrawableRes
import io.github.prithvimk.gymtracker.R

/** The tab's icon: a dumbbell, a clock turning back, a list and sliders. */
@get:DrawableRes
val TopLevelDestination.iconRes: Int
    get() =
        when (this) {
            TopLevelDestination.WORKOUT -> R.drawable.ic_tab_workout
            TopLevelDestination.HISTORY -> R.drawable.ic_tab_history
            TopLevelDestination.EXERCISES -> R.drawable.ic_tab_exercises
            TopLevelDestination.SETTINGS -> R.drawable.ic_tab_settings
        }
