package io.github.prithvimk.gymtracker.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.prithvimk.gymtracker.GymTrackerApp
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutExerciseEntry
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.repository.ExerciseRepository
import io.github.prithvimk.gymtracker.data.repository.WorkoutRepository
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseFilter
import io.github.prithvimk.gymtracker.ui.exercises.filteredBy
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface WorkoutUiState {
    data object Loading : WorkoutUiState

    data object Idle : WorkoutUiState

    data class Active(val workout: Workout, val exercises: List<WorkoutExerciseEntry> = emptyList()) : WorkoutUiState
}

/** The add-exercises sheet. [selectedIds] keeps tap order, which is the order the exercises are added in. */
data class ExercisePickerState(
    val query: String = "",
    val selectedIds: List<String> = emptyList(),
    val options: List<Exercise> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(private val workoutRepository: WorkoutRepository, exerciseRepository: ExerciseRepository,) :
    ViewModel() {
    // The active workout comes straight from the database, so it is back after the app is killed.
    val state: StateFlow<WorkoutUiState> =
        workoutRepository
            .observeActiveWorkout()
            .flatMapLatest { workout ->
                if (workout == null) {
                    flowOf(WorkoutUiState.Idle)
                } else {
                    workoutRepository.observeExerciseEntries(workout.id).map { WorkoutUiState.Active(workout, it) }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkoutUiState.Loading)

    private val _confirmingDiscard = MutableStateFlow(false)
    val confirmingDiscard: StateFlow<Boolean> = _confirmingDiscard.asStateFlow()

    // Null while the sheet is closed. Query and selection live here so typing never waits on the database.
    private val pickerInput = MutableStateFlow<ExercisePickerState?>(null)

    val picker: StateFlow<ExercisePickerState?> =
        combine(pickerInput, exerciseRepository.observeExercises()) { input, all ->
            input?.copy(options = all.filteredBy(ExerciseFilter(query = input.query)))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun onStartClick() {
        viewModelScope.launch { workoutRepository.startEmptyWorkout() }
    }

    fun onFinishClick() {
        val active = state.value as? WorkoutUiState.Active ?: return
        viewModelScope.launch { workoutRepository.finishWorkout(active.workout.id) }
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
        viewModelScope.launch { workoutRepository.deleteWorkout(active.workout.id) }
    }

    fun onAddExercisesClick() {
        pickerInput.value = ExercisePickerState()
    }

    fun onPickerQueryChange(query: String) {
        pickerInput.update { it?.copy(query = query) }
    }

    fun onPickerToggle(exerciseId: String) {
        pickerInput.update { it?.copy(selectedIds = it.selectedIds.toggled(exerciseId)) }
    }

    fun onPickerDismiss() {
        pickerInput.value = null
    }

    fun onPickerConfirm() {
        val selected = pickerInput.value?.selectedIds.orEmpty()
        val active = state.value as? WorkoutUiState.Active
        pickerInput.value = null
        if (active == null || selected.isEmpty()) return
        viewModelScope.launch { workoutRepository.addExercises(active.workout.id, selected) }
    }

    fun onMoveExercise(workoutExerciseId: String, offset: Int) {
        val active = state.value as? WorkoutUiState.Active ?: return
        viewModelScope.launch { workoutRepository.moveExercise(active.workout.id, workoutExerciseId, offset) }
    }

    fun onRemoveExercise(workoutExerciseId: String) {
        viewModelScope.launch { workoutRepository.deleteWorkoutExercise(workoutExerciseId) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymTrackerApp
                    WorkoutViewModel(app.container.workoutRepository, app.container.exerciseRepository)
                }
            }
    }
}

/** Adds [id] to the end, or removes it if it is already there. */
fun List<String>.toggled(id: String): List<String> = if (id in this) this - id else this + id

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
