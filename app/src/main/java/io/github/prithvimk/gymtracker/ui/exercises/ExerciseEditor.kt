package io.github.prithvimk.gymtracker.ui.exercises

import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.data.repository.ExerciseRepository
import io.github.prithvimk.gymtracker.data.repository.ExerciseSaveResult
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

/**
 * The create/edit dialog's state and actions, shared by the exercise list and an exercise's detail
 * screen. [state] is null while the dialog is closed. Saves run in [scope], the owning ViewModel's.
 */
class ExerciseEditor(private val repository: ExerciseRepository, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<ExerciseEditorState?>(null)
    val state: StateFlow<ExerciseEditorState?> = _state.asStateFlow()

    fun openNew(muscleGroup: MuscleGroup) {
        _state.value =
            ExerciseEditorState(
                exerciseId = null,
                name = "",
                muscleGroup = muscleGroup,
                equipment = Equipment.BARBELL,
                isCustom = true,
                archived = false,
            )
    }

    fun open(exercise: Exercise) {
        _state.value =
            ExerciseEditorState(
                exerciseId = exercise.id,
                name = exercise.name,
                muscleGroup = exercise.muscleGroup,
                equipment = exercise.equipment,
                isCustom = exercise.isCustom,
                archived = exercise.archived,
            )
    }

    fun onNameChange(name: String) {
        _state.update { it?.copy(name = name, error = null) }
    }

    fun onMuscleGroupChange(group: MuscleGroup) {
        _state.update { it?.copy(muscleGroup = group) }
    }

    fun onEquipmentChange(equipment: Equipment) {
        _state.update { it?.copy(equipment = equipment) }
    }

    fun onDismiss() {
        _state.value = null
    }

    fun onSave() {
        val state = _state.value ?: return
        if (state.isSaving || !state.isCustom) return
        _state.value = state.copy(isSaving = true)
        scope.launch {
            val result = save(state)
            if (result is ExerciseSaveResult.Saved) {
                _state.value = null
            } else {
                _state.update { it?.copy(isSaving = false, error = result.toEditorError()) }
            }
        }
    }

    fun onToggleArchived() {
        val state = _state.value ?: return
        val id = state.exerciseId ?: return
        if (state.isSaving) return
        _state.value = state.copy(isSaving = true)
        scope.launch {
            repository.setArchived(id, !state.archived)
            _state.value = null
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
}

private fun ExerciseSaveResult.toEditorError(): EditorError? = when (this) {
    is ExerciseSaveResult.Saved -> null
    ExerciseSaveResult.BlankName -> EditorError.BlankName
    is ExerciseSaveResult.DuplicateName -> EditorError.DuplicateName(existingName)
    ExerciseSaveResult.NotEditable -> EditorError.NotEditable
}
