package io.github.prithvimk.gymtracker.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.prithvimk.gymtracker.GymTrackerApp
import io.github.prithvimk.gymtracker.data.repository.WorkoutRepository
import io.github.prithvimk.gymtracker.timer.RestTimer
import io.github.prithvimk.gymtracker.timer.RestTimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What the mini bar shows: the running workout, if any, and the rest timer. */
class MiniBarViewModel(workoutRepository: WorkoutRepository, restTimer: RestTimer) : ViewModel() {
    val workout: StateFlow<MiniBarWorkout?> =
        miniBarWorkout(workoutRepository)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val rest: StateFlow<RestTimerState?> = restTimer.state

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymTrackerApp
                    MiniBarViewModel(app.container.workoutRepository, app.container.restTimer)
                }
            }
    }
}

/**
 * The running workout as the mini bar shows it, or null while none runs. Keyed on the workout and its
 * start, so edits to the workout row (its notes) don't look up the routine again.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun miniBarWorkout(workoutRepository: WorkoutRepository): Flow<MiniBarWorkout?> = workoutRepository
    .observeActiveWorkout()
    .map { it?.let { workout -> workout.id to workout.startedAt } }
    .distinctUntilChanged()
    .flatMapLatest { active ->
        if (active == null) {
            flowOf(null)
        } else {
            val (id, startedAt) = active
            workoutRepository.observeRoutineName(id).map { MiniBarWorkout(it, startedAt) }
        }
    }

/**
 * Whether the mini bar shows over the navigation bar, with a workout running. Not on the Workout tab,
 * where the logger shows all of it, nor while the keyboard is up or a screen such as the keypad has
 * [hidden] it.
 */
fun isMiniBarShown(route: String?, keyboardUp: Boolean, hidden: Boolean): Boolean =
    route != null && route != TopLevelDestination.WORKOUT.route && !keyboardUp && !hidden
