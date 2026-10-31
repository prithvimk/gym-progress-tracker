package io.github.prithvimk.gymtracker.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.prithvimk.gymtracker.GymTrackerApp
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.data.repository.ExerciseRepository
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ExerciseListState(
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    val hasArchived: Boolean = false,
)

class ExercisesViewModel(repository: ExerciseRepository) : ViewModel() {
    // Text field state lives in plain StateFlows so typing never waits on the database.
    private val _filter = MutableStateFlow(ExerciseFilter())
    val filter: StateFlow<ExerciseFilter> = _filter.asStateFlow()

    val editor = ExerciseEditor(repository, viewModelScope)

    private val listFlow =
        combine(repository.observeExercises(), _filter) { all, criteria ->
            ExerciseListState(
                exercises = all.filteredBy(criteria),
                isLoading = false,
                hasArchived = all.any { it.archived },
            )
        }

    val list: StateFlow<ExerciseListState> =
        listFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ExerciseListState())

    fun onQueryChange(query: String) {
        _filter.update { it.copy(query = query) }
    }

    fun onMuscleGroupSelect(group: MuscleGroup?) {
        _filter.update { it.copy(muscleGroup = group) }
    }

    fun onShowArchivedChange(show: Boolean) {
        _filter.update { it.copy(showArchived = show) }
    }

    fun onCreateClick() {
        editor.openNew(_filter.value.muscleGroup ?: MuscleGroup.CHEST)
    }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymTrackerApp
                    ExercisesViewModel(app.container.exerciseRepository)
                }
            }
    }
}
