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
import io.github.prithvimk.gymtracker.data.repository.IdleWorkoutResult
import io.github.prithvimk.gymtracker.data.repository.RoutineRepository
import io.github.prithvimk.gymtracker.data.repository.RoutineSaveResult
import io.github.prithvimk.gymtracker.data.repository.RoutineSummary
import io.github.prithvimk.gymtracker.data.repository.WorkoutRepository
import io.github.prithvimk.gymtracker.domain.DEFAULT_REST_SECONDS
import io.github.prithvimk.gymtracker.domain.MAX_REST_SECONDS
import io.github.prithvimk.gymtracker.domain.REST_STEP_SECONDS
import io.github.prithvimk.gymtracker.domain.WorkoutSummary
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.timer.RestTimer
import io.github.prithvimk.gymtracker.timer.RestTimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
     * (from the library) to its sets from the last finished workout that had it. [badges] maps each set
     * id that holds a record to the record types it holds.
     */
    data class Active(
        val workout: Workout,
        val exercises: List<WorkoutExerciseEntry> = emptyList(),
        val sets: Map<String, List<WorkoutSet>> = emptyMap(),
        val previousSets: Map<String, List<WorkoutSet>> = emptyMap(),
        val routineName: String? = null,
        val badges: Map<String, List<PrType>> = emptyMap(),
    ) : WorkoutUiState
}

/**
 * A finished workout's summary and the records it set, kept until it is dismissed. [endedAutomatically]
 * is true when it was left idle and ended by the app. [savingAsRoutine] is the routine name being typed.
 */
data class FinishedWorkout(
    val workoutId: String,
    val summary: WorkoutSummary,
    val records: List<ExerciseRecords> = emptyList(),
    val endedAutomatically: Boolean = false,
    val savingAsRoutine: String? = null,
    val routineNameError: Boolean = false,
)

/** The rest time being picked for one exercise of the workout. */
data class RestEdit(val workoutExerciseId: String, val exerciseName: String, val seconds: Int)

/** An exercise waiting on confirmation before it and its [setCount] sets are removed. */
data class PendingRemoval(val workoutExerciseId: String, val name: String, val setCount: Int)

/** The add-exercises sheet. [selectedIds] keeps tap order, which is the order the exercises are added in. */
data class ExercisePickerState(
    val query: String = "",
    val selectedIds: List<String> = emptyList(),
    val options: List<Exercise> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    private val routineRepository: RoutineRepository,
    private val restTimer: RestTimer,
) : ViewModel() {
    // The active workout comes straight from the database, so it is back after the app is killed.
    private val activeWorkout = workoutRepository.observeActiveWorkout()

    // Keyed on the id, so edits to the workout row itself (its notes) don't restart the queries below.
    val state: StateFlow<WorkoutUiState> =
        activeWorkout
            .map { it?.id }
            .distinctUntilChanged()
            .flatMapLatest { workoutId ->
                if (workoutId == null) {
                    flowOf(WorkoutUiState.Idle)
                } else {
                    val exercises = workoutRepository.observeExerciseEntries(workoutId)
                    // Past workouts don't change mid-session, so this is looked up only when the exercises change.
                    val previousSets =
                        exercises
                            .map { entries -> entries.map { it.exerciseId }.toSet() }
                            .distinctUntilChanged()
                            .mapLatest { ids -> ids.associateWith { workoutRepository.previousSets(it, workoutId) } }
                    val setsAndBadges =
                        combine(
                            workoutRepository.observeSetsForWorkout(workoutId),
                            workoutRepository.observeWorkoutRecords(workoutId),
                        ) { sets, records -> sets.groupBy { it.workoutExerciseId } to setBadges(records) }
                    combine(
                        activeWorkout.filter { it?.id == workoutId }.filterNotNull(),
                        exercises,
                        setsAndBadges,
                        previousSets,
                        workoutRepository.observeRoutineName(workoutId),
                    ) { workout, entries, (sets, badges), previous, routineName ->
                        WorkoutUiState.Active(workout, entries, sets, previous, routineName, badges)
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkoutUiState.Loading)

    // True while the notice that an idle workout with nothing done was deleted is showing.
    private val _idleDiscarded = MutableStateFlow(false)
    val idleDiscarded: StateFlow<Boolean> = _idleDiscarded.asStateFlow()

    private val _confirmingDiscard = MutableStateFlow(false)
    val confirmingDiscard: StateFlow<Boolean> = _confirmingDiscard.asStateFlow()

    // Held here because the finished workout is no longer active, so state has already moved on to Idle.
    private val _finished = MutableStateFlow<FinishedWorkout?>(null)
    val finished: StateFlow<FinishedWorkout?> = _finished.asStateFlow()

    val routines: StateFlow<List<RoutineSummary>> =
        routineRepository
            .observeRoutineSummaries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val rest: StateFlow<RestTimerState?> = restTimer.state

    private val _restEdit = MutableStateFlow<RestEdit?>(null)
    val restEdit: StateFlow<RestEdit?> = _restEdit.asStateFlow()

    /** Exercise, set and note edits to the active workout, and the keypad. */
    val editor =
        SessionEditor(workoutRepository, exerciseRepository, viewModelScope) {
            state.value as? WorkoutUiState.Active
        }

    fun onStartClick() {
        viewModelScope.launch { workoutRepository.startEmptyWorkout() }
    }

    fun onStartRoutineClick(routineId: String) {
        viewModelScope.launch { workoutRepository.startWorkoutFromRoutine(routineId) }
    }

    fun onFinishClick() {
        val active = state.value as? WorkoutUiState.Active ?: return
        editor.close()
        restTimer.skip()
        viewModelScope.launch {
            workoutRepository.finishWorkout(active.workout.id)
            showSummary(active.workout.id, endedAutomatically = false)
        }
    }

    /**
     * Called whenever the screen comes to the front: a workout left untouched for hours is ended at its
     * last activity and its summary shown, or deleted with a notice if nothing in it was done.
     */
    fun onResume() {
        viewModelScope.launch {
            when (val result = workoutRepository.endIdleWorkout()) {
                IdleWorkoutResult.StillActive -> return@launch
                is IdleWorkoutResult.Ended -> showSummary(result.workoutId, endedAutomatically = true)
                IdleWorkoutResult.Discarded -> _idleDiscarded.value = true
            }
            editor.close()
            restTimer.skip()
        }
    }

    fun onIdleDiscardedDismiss() {
        _idleDiscarded.value = false
    }

    private suspend fun showSummary(workoutId: String, endedAutomatically: Boolean) {
        val records = recordsByExercise(workoutRepository.observeWorkoutRecords(workoutId).first())
        _finished.value =
            workoutRepository.summarize(workoutId)?.let { FinishedWorkout(workoutId, it, records, endedAutomatically) }
    }

    fun onSummaryDismiss() {
        _finished.value = null
    }

    fun onSaveAsRoutineClick() {
        _finished.update { it?.copy(savingAsRoutine = "", routineNameError = false) }
    }

    fun onRoutineNameChange(name: String) {
        _finished.update { it?.copy(savingAsRoutine = name, routineNameError = false) }
    }

    fun onSaveRoutineDismiss() {
        _finished.update { it?.copy(savingAsRoutine = null, routineNameError = false) }
    }

    /** Saves the finished workout as a routine and closes the summary; it then shows in the routine list. */
    fun onSaveRoutineConfirm() {
        val finished = _finished.value ?: return
        val name = finished.savingAsRoutine ?: return
        viewModelScope.launch {
            when (routineRepository.createFromWorkout(finished.workoutId, name)) {
                is RoutineSaveResult.Saved -> _finished.value = null
                RoutineSaveResult.BlankName -> _finished.update { it?.copy(routineNameError = true) }
            }
        }
    }

    fun onDiscardClick() {
        _confirmingDiscard.value = true
    }

    fun onDiscardDismiss() {
        _confirmingDiscard.value = false
    }

    fun onDiscardConfirm() {
        _confirmingDiscard.value = false
        editor.close()
        restTimer.skip()
        val active = state.value as? WorkoutUiState.Active ?: return
        viewModelScope.launch { workoutRepository.deleteWorkout(active.workout.id) }
    }

    /** Ticking a set off starts the rest timer with that exercise's rest time. */
    fun onSetCompletedChange(setId: String, completed: Boolean) {
        editor.onSetCompletedChange(setId, completed)
        if (!completed) return
        val active = state.value as? WorkoutUiState.Active ?: return
        val set = active.sets.values.flatten().firstOrNull { it.id == setId } ?: return
        val entry = active.exercises.firstOrNull { it.id == set.workoutExerciseId } ?: return
        restTimer.start(entry.restSeconds ?: DEFAULT_REST_SECONDS, entry.name)
    }

    fun onRestAdjust(seconds: Int) {
        restTimer.adjust(seconds)
    }

    fun onRestSkip() {
        restTimer.skip()
    }

    fun onRestEditClick(workoutExerciseId: String) {
        val active = state.value as? WorkoutUiState.Active ?: return
        val entry = active.exercises.firstOrNull { it.id == workoutExerciseId } ?: return
        _restEdit.value = RestEdit(entry.id, entry.name, entry.restSeconds ?: DEFAULT_REST_SECONDS)
    }

    fun onRestEditStep(up: Boolean) {
        _restEdit.update { edit ->
            edit?.copy(
                seconds = (edit.seconds + if (up) REST_STEP_SECONDS else -REST_STEP_SECONDS).coerceIn(
                    0,
                    MAX_REST_SECONDS
                )
            )
        }
    }

    fun onRestEditDismiss() {
        _restEdit.value = null
    }

    fun onRestEditSave() {
        val edit = _restEdit.value ?: return
        _restEdit.value = null
        viewModelScope.launch { workoutRepository.updateExerciseRest(edit.workoutExerciseId, edit.seconds) }
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymTrackerApp
                    WorkoutViewModel(
                        app.container.workoutRepository,
                        app.container.exerciseRepository,
                        app.container.routineRepository,
                        app.container.restTimer,
                    )
                }
            }
    }
}

/** Adds [id] to the end, or removes it if it is already there. */
fun List<String>.toggled(id: String): List<String> = if (id in this) this - id else this + id
