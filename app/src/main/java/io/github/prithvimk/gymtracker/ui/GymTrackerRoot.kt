package io.github.prithvimk.gymtracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.prithvimk.gymtracker.timer.RestTimerState
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseDetailRoute
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseDetailViewModel
import io.github.prithvimk.gymtracker.ui.exercises.ExercisesRoute
import io.github.prithvimk.gymtracker.ui.history.HistoryRoute
import io.github.prithvimk.gymtracker.ui.history.WorkoutDetailRoute
import io.github.prithvimk.gymtracker.ui.history.WorkoutDetailViewModel
import io.github.prithvimk.gymtracker.ui.navigation.LocalMiniBarHiders
import io.github.prithvimk.gymtracker.ui.navigation.MiniBarHiders
import io.github.prithvimk.gymtracker.ui.navigation.MiniBarViewModel
import io.github.prithvimk.gymtracker.ui.navigation.MiniBarWorkout
import io.github.prithvimk.gymtracker.ui.navigation.TopLevelDestination
import io.github.prithvimk.gymtracker.ui.navigation.WorkoutMiniBar
import io.github.prithvimk.gymtracker.ui.navigation.iconRes
import io.github.prithvimk.gymtracker.ui.navigation.isMiniBarShown
import io.github.prithvimk.gymtracker.ui.navigation.tabEnterTransition
import io.github.prithvimk.gymtracker.ui.navigation.tabExitTransition
import io.github.prithvimk.gymtracker.ui.routines.RoutineEditorRoute
import io.github.prithvimk.gymtracker.ui.routines.RoutineEditorViewModel
import io.github.prithvimk.gymtracker.ui.screens.SettingsScreen
import io.github.prithvimk.gymtracker.ui.workout.WorkoutRoute

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GymTrackerRoot(miniBarViewModel: MiniBarViewModel = viewModel(factory = MiniBarViewModel.factory)) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val current = TopLevelDestination.fromRoute(route)
    val workout by miniBarViewModel.workout.collectAsStateWithLifecycle()
    val rest by miniBarViewModel.rest.collectAsStateWithLifecycle()
    val miniBarHiders = remember { MiniBarHiders() }

    val openTab: (TopLevelDestination) -> Unit = { destination ->
        if (destination == current) {
            // Tapping the current tab again goes back to its top, out of any screen under it.
            navController.popBackStack(destination.route, inclusive = false)
        } else {
            navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            AppBottomBar(
                current = current,
                onTabClick = openTab,
                miniBar = workout.takeIf {
                    isMiniBarShown(route, WindowInsets.isImeVisible, hidden = miniBarHiders.count > 0)
                },
                rest = rest,
                onMiniBarClick = {
                    openTab(TopLevelDestination.WORKOUT)
                    // Straight to the logger, even if the tab was left on a screen under it.
                    navController.popBackStack(TopLevelDestination.WORKOUT.route, inclusive = false)
                },
            )
        },
    ) { innerPadding ->
        CompositionLocalProvider(LocalMiniBarHiders provides miniBarHiders) {
            AppNavHost(navController, innerPadding)
        }
    }
}

/**
 * The navigation bar, with the [miniBar] over it while a workout runs elsewhere; it slides in and out.
 * [rest] is the rest timer the mini bar counts down.
 */
@Composable
fun AppBottomBar(
    current: TopLevelDestination?,
    onTabClick: (TopLevelDestination) -> Unit,
    miniBar: MiniBarWorkout?,
    rest: RestTimerState?,
    onMiniBarClick: () -> Unit,
) {
    Column(modifier = Modifier.background(NavigationBarDefaults.containerColor)) {
        // Kept while it slides out, so it doesn't go blank on the way.
        var lastMiniBar by remember { mutableStateOf(miniBar) }
        if (miniBar != null) lastMiniBar = miniBar
        AnimatedVisibility(
            visible = miniBar != null,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            lastMiniBar?.let {
                WorkoutMiniBar(
                    workout = it,
                    rest = rest,
                    onClick = onMiniBarClick,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp),
                )
            }
        }
        NavigationBar {
            TopLevelDestination.entries.forEach { destination ->
                NavigationBarItem(
                    selected = destination == current,
                    onClick = { onTabClick(destination) },
                    icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                    label = { Text(stringResource(destination.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, innerPadding: PaddingValues) {
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
        composable(TopLevelDestination.HISTORY.route) {
            HistoryRoute(onWorkoutClick = { navController.navigate(workoutDetailRoute(it)) })
        }
        composable(
            WORKOUT_DETAIL_ROUTE,
            arguments = listOf(navArgument(WORKOUT_ID) { type = NavType.StringType }),
        ) {
            WorkoutDetailRoute(
                onBack = { navController.popBackStack() },
                onExerciseClick = { navController.navigate(historyExerciseRoute(it)) },
            )
        }
        // The same exercise detail as under Exercises, kept under History so that tab stays selected.
        composable(
            HISTORY_EXERCISE_ROUTE,
            arguments = listOf(navArgument(EXERCISE_ID) { type = NavType.StringType }),
        ) {
            ExerciseDetailRoute(onBack = { navController.popBackStack() })
        }
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

private const val ROUTINE_ID = RoutineEditorViewModel.ROUTINE_ID_ARG
private const val ROUTINE_EDITOR_ROUTE = "workout/routine?$ROUTINE_ID={$ROUTINE_ID}"

private const val EXERCISE_ID = ExerciseDetailViewModel.EXERCISE_ID_ARG
private const val EXERCISE_DETAIL_ROUTE = "exercises/{$EXERCISE_ID}"

private fun exerciseDetailRoute(id: String): String = "exercises/$id"

private const val HISTORY_EXERCISE_ROUTE = "history/exercise/{$EXERCISE_ID}"

private fun historyExerciseRoute(id: String): String = "history/exercise/$id"

private const val WORKOUT_ID = WorkoutDetailViewModel.WORKOUT_ID_ARG
private const val WORKOUT_DETAIL_ROUTE = "history/{$WORKOUT_ID}"

private fun workoutDetailRoute(id: String): String = "history/$id"

/** The routine editor for routine [id], or for a new routine when [id] is null. */
private fun routineEditorRoute(id: String?): String = if (id ==
    null
) {
    "workout/routine"
} else {
    "workout/routine?$ROUTINE_ID=$id"
}
