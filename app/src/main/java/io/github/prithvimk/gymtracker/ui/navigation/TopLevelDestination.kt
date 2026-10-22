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

        /** The tab a route belongs to: the tab itself, or a screen under it such as "workout/routine". */
        fun fromRoute(route: String?): TopLevelDestination? =
            entries.firstOrNull { route == it.route || route?.startsWith("${it.route}/") == true }
    }
}
