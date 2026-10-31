package io.github.prithvimk.gymtracker.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.prithvimk.gymtracker.GymTrackerApp
import io.github.prithvimk.gymtracker.data.repository.ExerciseRepository
import io.github.prithvimk.gymtracker.data.repository.WorkoutRepository
import io.github.prithvimk.gymtracker.domain.WorkoutSummary
import io.github.prithvimk.gymtracker.domain.summarizeWorkout
import io.github.prithvimk.gymtracker.ui.workout.ExerciseRecords
import io.github.prithvimk.gymtracker.ui.workout.SessionEditor
import io.github.prithvimk.gymtracker.ui.workout.WorkoutUiState
import io.github.prithvimk.gymtracker.ui.workout.recordsByExercise
import io.github.prithvimk.gymtracker.ui.workout.setBadges
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WorkoutDetailState {
    data object Loading : WorkoutDetailState

    /** No workout has the id, or it was just deleted, so the screen closes. */
    data object Missing : WorkoutDetailState

    /**
     * [session] is the workout as the logger would show it, with "last time" taken from the session
     * before this one. [exercises] is what was done, for reading; [records] are the records it holds,
     * by exercise. [editing] switches the screen to the logger's editor.
     */
    data class Loaded(
        val session: WorkoutUiState.Active,
        val summary: WorkoutSummary,
        val exercises: List<DoneExercise>,
        val records: List<ExerciseRecords>,
        val editing: Boolean,
    ) : WorkoutDetailState
}

/**
 * A past workout: what was done in it, and edits to it. Edits save as they are made, the same as in
 * the logger, and its exercises' records are worked out again after each one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val workoutId: String = checkNotNull(savedStateHandle.get<String>(WORKOUT_ID_ARG))

    // Kept across process death, so the app comes back to the editor if it was killed mid-edit.
    private val editing = savedStateHandle.getStateFlow(EDITING_KEY, false)
    private val setEditing: (Boolean) -> Unit = { savedStateHandle[EDITING_KEY] = it }

    private val workout = workoutRepository.observeWorkout(workoutId)

    private val session =
        run {
            val exercises = workoutRepository.observeExerciseEntries(workoutId)
            // Earlier workouts don't change while this one is open, so this follows only its exercises and start.
            val previousSets =
                combine(
                    exercises.map { entries -> entries.map { it.exerciseId }.toSet() }.distinctUntilChanged(),
                    workout.filterNotNull().map { it.startedAt }.distinctUntilChanged(),
                ) { ids, _ -> ids }
                    .mapLatest { ids -> ids.associateWith { workoutRepository.setsBefore(it, workoutId) } }
            combine(
                workout.filterNotNull(),
                exercises,
                workoutRepository.observeSetsForWorkout(workoutId),
                previousSets,
                workoutRepository.observeRoutineName(workoutId),
            ) { workout, entries, sets, previous, routineName ->
                WorkoutUiState.Active(workout, entries, sets.groupBy { it.workoutExerciseId }, previous, routineName)
            }
        }

    val state: StateFlow<WorkoutDetailState> =
        workout
            .map { it != null }
            .distinctUntilChanged()
            .flatMapLatest { exists ->
                if (!exists) {
                    flowOf(WorkoutDetailState.Missing)
                } else {
                    combine(session, workoutRepository.observeWorkoutRecords(workoutId), editing) {
                            current,
                            records,
                            isEditing,
                        ->
                        val sets = current.sets.values.flatten()
                        val badges = setBadges(records)
                        WorkoutDetailState.Loaded(
                            session = current.copy(badges = badges),
                            summary = summarizeWorkout(current.workout, sets, now = current.workout.endedAt ?: 0L),
                            exercises = doneExercises(current.exercises, current.sets, badges),
                            records = recordsByExercise(records),
                            editing = isEditing,
                        )
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkoutDetailState.Loading)

    val editor =
        SessionEditor(workoutRepository, exerciseRepository, viewModelScope) {
            (state.value as? WorkoutDetailState.Loaded)?.session
        }

    private val _confirmingDelete = MutableStateFlow(false)
    val confirmingDelete: StateFlow<Boolean> = _confirmingDelete.asStateFlow()

    fun onEditClick() {
        setEditing(true)
    }

    fun onDoneClick() {
        editor.close()
        setEditing(false)
    }

    fun onDeleteClick() {
        _confirmingDelete.value = true
    }

    fun onDeleteDismiss() {
        _confirmingDelete.value = false
    }

    /** Deletes the workout; the state then turns [WorkoutDetailState.Missing] and the screen closes. */
    fun onDeleteConfirm() {
        _confirmingDelete.value = false
        editor.close()
        viewModelScope.launch { workoutRepository.deleteWorkout(workoutId) }
    }

    companion object {
        const val WORKOUT_ID_ARG = "workoutId"
        private const val EDITING_KEY = "editing"
        private const val STOP_TIMEOUT_MS = 5_000L

        val factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymTrackerApp
                    WorkoutDetailViewModel(
                        createSavedStateHandle(),
                        app.container.workoutRepository,
                        app.container.exerciseRepository,
                    )
                }
            }
    }
}
