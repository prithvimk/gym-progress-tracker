package io.github.prithvimk.gymtracker.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseDetailRoute
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseDetailViewModel
import io.github.prithvimk.gymtracker.ui.exercises.ExercisesRoute
import io.github.prithvimk.gymtracker.ui.history.HistoryRoute
import io.github.prithvimk.gymtracker.ui.navigation.TopLevelDestination
import io.github.prithvimk.gymtracker.ui.navigation.icon
import io.github.prithvimk.gymtracker.ui.navigation.tabEnterTransition
import io.github.prithvimk.gymtracker.ui.navigation.tabExitTransition
import io.github.prithvimk.gymtracker.ui.routines.RoutineEditorRoute
import io.github.prithvimk.gymtracker.ui.routines.RoutineEditorViewModel
import io.github.prithvimk.gymtracker.ui.screens.SettingsScreen
import io.github.prithvimk.gymtracker.ui.workout.WorkoutRoute

@Composable
fun GymTrackerRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = TopLevelDestination.fromRoute(backStackEntry?.destination?.route)

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = destination == current,
                        onClick = {
                            // Tapping the current tab again goes back to its top, out of any screen under it.
                            if (destination == current) {
                                navController.popBackStack(destination.route, inclusive = false)
                                return@NavigationBarItem
                            }
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.START.route,
            // Edge-to-edge means the window no longer shrinks for the keyboard, so the content makes room
            // itself. The bottom bar's share of innerPadding is consumed first so it isn't counted twice.
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).imePadding(),
            enterTransition = { tabEnterTransition },
            exitTransition = { tabExitTransition },
            popEnterTransition = { tabEnterTransition },
            popExitTransition = { tabExitTransition },
        ) {
            composable(TopLevelDestination.WORKOUT.route) {
                WorkoutRoute(onEditRoutine = { navController.navigate(routineEditorRoute(it)) })
            }
            composable(
                ROUTINE_EDITOR_ROUTE,
                arguments =
                listOf(
                    navArgument(RoutineEditorViewModel.ROUTINE_ID_ARG) {
                        type = NavType.StringType
                        nullable = true
                    },
                ),
            ) {
                RoutineEditorRoute(onDone = { navController.popBackStack() })
            }
            composable(TopLevelDestination.HISTORY.route) { HistoryRoute() }
            composable(TopLevelDestination.EXERCISES.route) {
                ExercisesRoute(onExerciseClick = { navController.navigate(exerciseDetailRoute(it.id)) })
            }
            composable(
                EXERCISE_DETAIL_ROUTE,
                arguments = listOf(navArgument(EXERCISE_ID) { type = NavType.StringType }),
            ) {
                ExerciseDetailRoute(onBack = { navController.popBackStack() })
            }
            composable(TopLevelDestination.SETTINGS.route) { SettingsScreen() }
        }
    }
}

private const val ROUTINE_ID = RoutineEditorViewModel.ROUTINE_ID_ARG
private const val ROUTINE_EDITOR_ROUTE = "workout/routine?$ROUTINE_ID={$ROUTINE_ID}"

private const val EXERCISE_ID = ExerciseDetailViewModel.EXERCISE_ID_ARG
private const val EXERCISE_DETAIL_ROUTE = "exercises/{$EXERCISE_ID}"

private fun exerciseDetailRoute(id: String): String = "exercises/$id"

/** The routine editor for routine [id], or for a new routine when [id] is null. */
private fun routineEditorRoute(id: String?): String = if (id ==
    null
) {
    "workout/routine"
} else {
    "workout/routine?$ROUTINE_ID=$id"
}
