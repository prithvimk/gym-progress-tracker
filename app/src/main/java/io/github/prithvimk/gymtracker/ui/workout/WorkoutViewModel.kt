package io.github.prithvimk.gymtracker.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.prithvimk.gymtracker.GymTrackerApp
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.repository.WorkoutRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WorkoutUiState {
    data object Loading : WorkoutUiState

    data object Idle : WorkoutUiState

    data class Active(val workout: Workout) : WorkoutUiState
}

class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {
    // The active workout comes straight from the database, so it is back after the app is killed.
    val state: StateFlow<WorkoutUiState> =
        repository
            .observeActiveWorkout()
            .map { workout -> workout?.let(WorkoutUiState::Active) ?: WorkoutUiState.Idle }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkoutUiState.Loading)

    private val _confirmingDiscard = MutableStateFlow(false)
    val confirmingDiscard: StateFlow<Boolean> = _confirmingDiscard.asStateFlow()

    fun onStartClick() {
        viewModelScope.launch { repository.startEmptyWorkout() }
    }

    fun onFinishClick() {
        val active = state.value as? WorkoutUiState.Active ?: return
        viewModelScope.launch { repository.finishWorkout(active.workout.id) }
    }

    fun onDiscardClick() {
        _confirmingDiscard.value = true
    }

    fun onDiscardDismiss() {
        _confirmingDiscard.value = false
    }

    fun onDiscardConfirm() {
        _confirmingDiscard.value = false
        val active = state.value as? WorkoutUiState.Active ?: return
        viewModelScope.launch { repository.deleteWorkout(active.workout.id) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymTrackerApp
                    WorkoutViewModel(app.container.workoutRepository)
                }
            }
    }
}

/** Formats elapsed time as "m:ss", or "h:mm:ss" from one hour. Negative input counts as zero. */
fun formatElapsed(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = totalSeconds % 3_600L / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}
