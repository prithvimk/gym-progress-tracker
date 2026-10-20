package io.github.prithvimk.gymtracker.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.prithvimk.gymtracker.GymTrackerApp
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.data.repository.ExerciseRepository
import io.github.prithvimk.gymtracker.data.repository.ExerciseSaveResult
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseListState(
    val exercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = true,
    val hasArchived: Boolean = false,
)

/** The create/edit dialog. A null [exerciseId] means a new exercise. */
data class ExerciseEditorState(
    val exerciseId: String?,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val isCustom: Boolean,
    val archived: Boolean,
    val error: EditorError? = null,
    val isSaving: Boolean = false,
) {
    val isNew: Boolean
        get() = exerciseId == null
}

sealed interface EditorError {
    data object BlankName : EditorError

    data class DuplicateName(val existingName: String) : EditorError

    data object NotEditable : EditorError
}

class ExercisesViewModel(private val repository: ExerciseRepository) : ViewModel() {
    // Text field state lives in plain StateFlows so typing never waits on the database.
    private val _filter = MutableStateFlow(ExerciseFilter())
    val filter: StateFlow<ExerciseFilter> = _filter.asStateFlow()

    private val _editor = MutableStateFlow<ExerciseEditorState?>(null)
    val editor: StateFlow<ExerciseEditorState?> = _editor.asStateFlow()

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
        _editor.value =
            ExerciseEditorState(
                exerciseId = null,
                name = "",
                muscleGroup = _filter.value.muscleGroup ?: MuscleGroup.CHEST,
                equipment = Equipment.BARBELL,
                isCustom = true,
                archived = false,
            )
    }

    fun onExerciseClick(exercise: Exercise) {
        _editor.value =
            ExerciseEditorState(
                exerciseId = exercise.id,
                name = exercise.name,
                muscleGroup = exercise.muscleGroup,
                equipment = exercise.equipment,
                isCustom = exercise.isCustom,
                archived = exercise.archived,
            )
    }

    fun onEditorNameChange(name: String) {
        _editor.update { it?.copy(name = name, error = null) }
    }

    fun onEditorMuscleGroupChange(group: MuscleGroup) {
        _editor.update { it?.copy(muscleGroup = group) }
    }

    fun onEditorEquipmentChange(equipment: Equipment) {
        _editor.update { it?.copy(equipment = equipment) }
    }

    fun onEditorDismiss() {
        _editor.value = null
    }

    fun onEditorSave() {
        val state = _editor.value ?: return
        if (state.isSaving || !state.isCustom) return
        _editor.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val result = save(state)
            if (result is ExerciseSaveResult.Saved) {
                _editor.value = null
            } else {
                _editor.update { it?.copy(isSaving = false, error = result.toEditorError()) }
            }
        }
    }

    fun onEditorToggleArchived() {
        val state = _editor.value ?: return
        val id = state.exerciseId ?: return
        if (state.isSaving) return
        _editor.value = state.copy(isSaving = true)
        viewModelScope.launch {
            repository.setArchived(id, !state.archived)
            _editor.value = null
        }
    }

    private suspend fun save(state: ExerciseEditorState): ExerciseSaveResult {
        val id = state.exerciseId
        return if (id == null) {
            repository.createCustom(state.name, state.muscleGroup, state.equipment)
        } else {
            repository.updateCustom(id, state.name, state.muscleGroup, state.equipment)
        }
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

private fun ExerciseSaveResult.toEditorError(): EditorError? = when (this) {
    is ExerciseSaveResult.Saved -> null
    ExerciseSaveResult.BlankName -> EditorError.BlankName
    is ExerciseSaveResult.DuplicateName -> EditorError.DuplicateName(existingName)
    ExerciseSaveResult.NotEditable -> EditorError.NotEditable
}
