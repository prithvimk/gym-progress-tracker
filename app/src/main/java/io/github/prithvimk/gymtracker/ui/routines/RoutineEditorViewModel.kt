package io.github.prithvimk.gymtracker.ui.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.prithvimk.gymtracker.GymTrackerApp
import io.github.prithvimk.gymtracker.data.repository.ExerciseRepository
import io.github.prithvimk.gymtracker.data.repository.RoutineExerciseInput
import io.github.prithvimk.gymtracker.data.repository.RoutineRepository
import io.github.prithvimk.gymtracker.data.repository.RoutineSaveResult
import io.github.prithvimk.gymtracker.domain.ExerciseTargets
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseFilter
import io.github.prithvimk.gymtracker.ui.exercises.filteredBy
import io.github.prithvimk.gymtracker.ui.workout.ExercisePickerState
import io.github.prithvimk.gymtracker.ui.workout.toggled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The routine editor. Edits stay in memory until saved. [dirty] is whether there is anything unsaved;
 * [done] turns true once the editor should close.
 */
data class RoutineEditorState(
    val isNew: Boolean,
    val loading: Boolean = !isNew,
    val name: String = "",
    val notes: String = "",
    val exercises: List<DraftExercise> = emptyList(),
    val nameError: Boolean = false,
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val confirmingDiscard: Boolean = false,
    val confirmingDelete: Boolean = false,
    val done: Boolean = false,
)

class RoutineEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val routineRepository: RoutineRepository,
    exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val routineId: String? = savedStateHandle[ROUTINE_ID_ARG]

    private val _state = MutableStateFlow(RoutineEditorState(isNew = routineId == null))
    val state: StateFlow<RoutineEditorState> = _state.asStateFlow()

    private var nextKey = 0

    private val pickerInput = MutableStateFlow<ExercisePickerState?>(null)

    val picker: StateFlow<ExercisePickerState?> =
        combine(pickerInput, exerciseRepository.observeExercises()) { input, all ->
            input?.copy(options = all.filteredBy(ExerciseFilter(query = input.query)))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    // Names and details of library exercises, for exercises added from the picker.
    private val library = exerciseRepository.observeExercises()

    init {
        if (routineId != null) {
            viewModelScope.launch {
                val detail = routineRepository.getRoutineDetail(routineId)
                if (detail == null) {
                    _state.update { it.copy(done = true) }
                    return@launch
                }
                _state.update {
                    it.copy(
                        loading = false,
                        name = detail.routine.name,
                        notes = detail.routine.notes,
                        exercises =
                        detail.exercises.map { entry ->
                            DraftExercise(
                                key = entry.id,
                                id = entry.id,
                                exerciseId = entry.exerciseId,
                                name = entry.name,
                                muscleGroup = entry.muscleGroup,
                                equipment = entry.equipment,
                                targets = ExerciseTargets(
                                    entry.targetSets,
                                    entry.repMin,
                                    entry.repMax,
                                    entry.restSeconds
                                ),
                            )
                        },
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        edit { it.copy(name = name, nameError = false) }
    }

    fun onNotesChange(notes: String) {
        edit { it.copy(notes = notes) }
    }

    fun onStep(key: String, field: TargetField, up: Boolean) {
        editExercises { list ->
            list.map {
                if (it.key ==
                    key
                ) {
                    it.copy(targets = it.targets.stepped(field, up))
                } else {
                    it
                }
            }
        }
    }

    fun onMove(key: String, offset: Int) {
        editExercises { list -> list.moved(list.indexOfFirst { it.key == key }, offset) }
    }

    fun onRemove(key: String) {
        editExercises { list -> list.filterNot { it.key == key } }
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
        pickerInput.value = null
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val byId = library.first().associateBy { it.id }
            val added =
                selected.mapNotNull { byId[it] }.map { exercise ->
                    DraftExercise(
                        key = "new-${nextKey++}",
                        id = null,
                        exerciseId = exercise.id,
                        name = exercise.name,
                        muscleGroup = exercise.muscleGroup,
                        equipment = exercise.equipment,
                        targets = ExerciseTargets.Default,
                    )
                }
            editExercises { it + added }
        }
    }

    fun onSave() {
        val current = _state.value
        if (current.saving || current.loading) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result =
                routineRepository.saveRoutine(
                    id = routineId,
                    name = current.name,
                    notes = current.notes,
                    exercises = current.exercises.map { RoutineExerciseInput(it.id, it.exerciseId, it.targets) },
                )
            _state.update {
                when (result) {
                    is RoutineSaveResult.Saved -> it.copy(saving = false, dirty = false, done = true)
                    RoutineSaveResult.BlankName -> it.copy(saving = false, nameError = true)
                }
            }
        }
    }

    /** Back: closes straight away if nothing changed, otherwise asks first. */
    fun onBack() {
        if (_state.value.dirty) {
            _state.update { it.copy(confirmingDiscard = true) }
        } else {
            _state.update { it.copy(done = true) }
        }
    }

    fun onDiscardConfirm() {
        _state.update { it.copy(confirmingDiscard = false, dirty = false, done = true) }
    }

    fun onDiscardDismiss() {
        _state.update { it.copy(confirmingDiscard = false) }
    }

    fun onDeleteClick() {
        _state.update { it.copy(confirmingDelete = true) }
    }

    fun onDeleteDismiss() {
        _state.update { it.copy(confirmingDelete = false) }
    }

    fun onDeleteConfirm() {
        val id = routineId ?: return
        _state.update { it.copy(confirmingDelete = false, saving = true) }
        viewModelScope.launch {
            routineRepository.deleteRoutine(id)
            _state.update { it.copy(saving = false, dirty = false, done = true) }
        }
    }

    private fun edit(change: (RoutineEditorState) -> RoutineEditorState) {
        _state.update { change(it).copy(dirty = true) }
    }

    private fun editExercises(change: (List<DraftExercise>) -> List<DraftExercise>) {
        edit { it.copy(exercises = change(it.exercises)) }
    }

    companion object {
        const val ROUTINE_ID_ARG = "routineId"
        private const val STOP_TIMEOUT_MS = 5_000L

        val factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GymTrackerApp
                    RoutineEditorViewModel(
                        createSavedStateHandle(),
                        app.container.routineRepository,
                        app.container.exerciseRepository,
                    )
                }
            }
    }
}
