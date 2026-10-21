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
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.data.repository.ExerciseRepository
import io.github.prithvimk.gymtracker.data.repository.WorkoutRepository
import io.github.prithvimk.gymtracker.domain.model.SetType
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseFilter
import io.github.prithvimk.gymtracker.ui.exercises.filteredBy
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface WorkoutUiState {
    data object Loading : WorkoutUiState

    data object Idle : WorkoutUiState

    /**
     * [sets] maps each workout exercise id to its sets, in order. [previousSets] maps each exercise id
     * (from the library) to its sets from the last finished workout that had it.
     */
    data class Active(
        val workout: Workout,
        val exercises: List<WorkoutExerciseEntry> = emptyList(),
        val sets: Map<String, List<WorkoutSet>> = emptyMap(),
        val previousSets: Map<String, List<WorkoutSet>> = emptyMap(),
    ) : WorkoutUiState
}

/** An exercise waiting on confirmation before it and its [setCount] sets are removed. */
data class PendingRemoval(val workoutExerciseId: String, val name: String, val setCount: Int)

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
                    val exercises = workoutRepository.observeExerciseEntries(workout.id)
                    // Past workouts don't change mid-session, so this is looked up only when the exercises change.
                    val previousSets =
                        exercises
                            .map { entries -> entries.map { it.exerciseId }.toSet() }
                            .distinctUntilChanged()
                            .mapLatest { ids -> ids.associateWith { workoutRepository.previousSets(it, workout.id) } }
                    combine(
                        exercises,
                        workoutRepository.observeSetsForWorkout(workout.id),
                        previousSets,
                    ) { entries, sets, previous ->
                        WorkoutUiState.Active(workout, entries, sets.groupBy { it.workoutExerciseId }, previous)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkoutUiState.Loading)

    private val _confirmingDiscard = MutableStateFlow(false)
    val confirmingDiscard: StateFlow<Boolean> = _confirmingDiscard.asStateFlow()

    private val _pendingRemoval = MutableStateFlow<PendingRemoval?>(null)
    val pendingRemoval: StateFlow<PendingRemoval?> = _pendingRemoval.asStateFlow()

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

    /** Removes the exercise straight away if it has no sets; otherwise asks first. */
    fun onRemoveExercise(workoutExerciseId: String) {
        val active = state.value as? WorkoutUiState.Active ?: return
        val setCount = active.sets[workoutExerciseId].orEmpty().size
        if (setCount == 0) {
            viewModelScope.launch { workoutRepository.deleteWorkoutExercise(workoutExerciseId) }
        } else {
            val name = active.exercises.firstOrNull { it.id == workoutExerciseId }?.name ?: return
            _pendingRemoval.value = PendingRemoval(workoutExerciseId, name, setCount)
        }
    }

    fun onRemoveConfirm() {
        val pending = _pendingRemoval.value ?: return
        _pendingRemoval.value = null
        viewModelScope.launch { workoutRepository.deleteWorkoutExercise(pending.workoutExerciseId) }
    }

    fun onRemoveDismiss() {
        _pendingRemoval.value = null
    }

    fun onAddSet(workoutExerciseId: String) {
        viewModelScope.launch { workoutRepository.addSet(workoutExerciseId) }
    }

    fun onSetWeightChange(setId: String, weightKg: Double) {
        viewModelScope.launch { workoutRepository.updateSetWeight(setId, weightKg) }
    }

    fun onSetRepsChange(setId: String, reps: Int) {
        viewModelScope.launch { workoutRepository.updateSetReps(setId, reps) }
    }

    fun onSetRpeChange(setId: String, rpe: Double?) {
        viewModelScope.launch { workoutRepository.updateSetRpe(setId, rpe) }
    }

    fun onSetTypeChange(setId: String, type: SetType) {
        viewModelScope.launch { workoutRepository.updateSetType(setId, type) }
    }

    fun onSetCompletedChange(setId: String, completed: Boolean) {
        viewModelScope.launch { workoutRepository.setSetCompleted(setId, completed) }
    }

    fun onDeleteSet(setId: String) {
        viewModelScope.launch { workoutRepository.deleteSet(setId) }
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
