package io.github.prithvimk.gymtracker.ui.navigation

import androidx.annotation.StringRes
import io.github.prithvimk.gymtracker.R

/** Tabs in the bottom navigation bar, in display order. */
enum class TopLevelDestination(val route: String, @param:StringRes val labelRes: Int) {
    WORKOUT(route = "workout", labelRes = R.string.nav_workout),
    HISTORY(route = "history", labelRes = R.string.nav_history),
    EXERCISES(route = "exercises", labelRes = R.string.nav_exercises),
    SETTINGS(route = "settings", labelRes = R.string.nav_settings),
    ;

    companion object {
        val START = WORKOUT

        fun fromRoute(route: String?): TopLevelDestination? = entries.firstOrNull { it.route == route }
    }
}
