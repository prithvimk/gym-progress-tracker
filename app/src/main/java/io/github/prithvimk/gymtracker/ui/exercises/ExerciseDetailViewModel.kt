package io.github.prithvimk.gymtracker.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.prithvimk.gymtracker.GymTrackerApp
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.data.repository.ExerciseRepository
import io.github.prithvimk.gymtracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface ExerciseDetailState {
    data object Loading : ExerciseDetailState

    /** No exercise has the id, so there is nothing to show and the screen closes. */
    data object Missing : ExerciseDetailState

    /** [sessions] is newest first, and empty until a set of the exercise is completed. */
    data class Loaded(val exercise: Exercise, val records: List<RecordBest>, val sessions: List<HistorySession>) :
        ExerciseDetailState
}

class ExerciseDetailViewModel(
    savedStateHandle: SavedStateHandle,
    exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository,
) : ViewModel() {
    private val exerciseId: String = checkNotNull(savedStateHandle.get<String>(EXERCISE_ID_ARG))

    val editor = ExerciseEditor(exerciseRepository, viewModelScope)

    val state: StateFlow<ExerciseDetailState> =
        combine(
            exerciseRepository.observeExercise(exerciseId),
            workoutRepository.observeExerciseHistory(exerciseId),
            workoutRepository.observePersonalRecords(exerciseId),
        ) { exercise, rows, records ->
            if (exercise == null) {
                ExerciseDetailState.Missing
            } else {
                ExerciseDetailState.Loaded(exercise, standingRecords(records, rows), historySessions(rows, records))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ExerciseDetailState.Loading)

    fun onEditClick() {
        val loaded = state.value as? ExerciseDetailState.Loaded ?: return
        editor.open(loaded.exercise)
    }

    companion object {
        const val EXERCISE_ID_ARG = "exerciseId"
        private const val STOP_TIMEOUT_MS = 5_000L

        val factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymTrackerApp
                    ExerciseDetailViewModel(
                        createSavedStateHandle(),
                        app.container.exerciseRepository,
                        app.container.workoutRepository,
                    )
                }
            }
    }
}
