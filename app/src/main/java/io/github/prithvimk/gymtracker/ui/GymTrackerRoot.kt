package io.github.prithvimk.gymtracker.ui

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.prithvimk.gymtracker.ui.exercises.ExercisesRoute
import io.github.prithvimk.gymtracker.ui.navigation.TopLevelDestination
import io.github.prithvimk.gymtracker.ui.navigation.icon
import io.github.prithvimk.gymtracker.ui.navigation.tabEnterTransition
import io.github.prithvimk.gymtracker.ui.navigation.tabExitTransition
import io.github.prithvimk.gymtracker.ui.screens.HistoryScreen
import io.github.prithvimk.gymtracker.ui.screens.SettingsScreen
import io.github.prithvimk.gymtracker.ui.screens.WorkoutScreen

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
            modifier = Modifier.padding(innerPadding),
            enterTransition = { tabEnterTransition },
            exitTransition = { tabExitTransition },
            popEnterTransition = { tabEnterTransition },
            popExitTransition = { tabExitTransition },
        ) {
            composable(TopLevelDestination.WORKOUT.route) { WorkoutScreen() }
            composable(TopLevelDestination.HISTORY.route) { HistoryScreen() }
            composable(TopLevelDestination.EXERCISES.route) { ExercisesRoute() }
            composable(TopLevelDestination.SETTINGS.route) { SettingsScreen() }
        }
    }
}
