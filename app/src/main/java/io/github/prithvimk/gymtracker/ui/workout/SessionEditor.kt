package io.github.prithvimk.gymtracker.ui.workout

import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.data.repository.ExerciseRepository
import io.github.prithvimk.gymtracker.data.repository.WorkoutRepository
import io.github.prithvimk.gymtracker.domain.model.SetType
import io.github.prithvimk.gymtracker.ui.exercises.ExerciseFilter
import io.github.prithvimk.gymtracker.ui.exercises.filteredBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Edits to one workout's exercises, sets and notes, shared by the logger and the past-workout editor:
 * the keypad, the add-exercises sheet and the confirmation before an exercise with sets is removed.
 * [session] is the workout as it is shown, or null while there is none.
 */
class SessionEditor(
    private val workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository,
    private val scope: CoroutineScope,
    private val session: () -> WorkoutUiState.Active?,
) {
    // The keypad's edit, or null while it is closed.
    private val _keypad = MutableStateFlow<KeypadState?>(null)
    val keypad: StateFlow<KeypadState?> = _keypad.asStateFlow()

    private val _pendingRemoval = MutableStateFlow<PendingRemoval?>(null)
    val pendingRemoval: StateFlow<PendingRemoval?> = _pendingRemoval.asStateFlow()

    // Null while the sheet is closed. Query and selection live here so typing never waits on the database.
    private val pickerInput = MutableStateFlow<ExercisePickerState?>(null)

    val picker: StateFlow<ExercisePickerState?> =
        combine(pickerInput, exerciseRepository.observeExercises()) { input, all ->
            input?.copy(options = all.filteredBy(ExerciseFilter(query = input.query)))
        }.stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

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
        val workoutId = session()?.workout?.id
        pickerInput.value = null
        if (workoutId == null || selected.isEmpty()) return
        scope.launch { workoutRepository.addExercises(workoutId, selected) }
    }

    fun onMoveExercise(workoutExerciseId: String, offset: Int) {
        val workoutId = session()?.workout?.id ?: return
        scope.launch { workoutRepository.moveExercise(workoutId, workoutExerciseId, offset) }
    }

    /** Removes the exercise straight away if it has no sets; otherwise asks first. */
    fun onRemoveExercise(workoutExerciseId: String) {
        val current = session() ?: return
        val setCount = current.sets[workoutExerciseId].orEmpty().size
        if (setCount == 0) {
            scope.launch { workoutRepository.deleteWorkoutExercise(workoutExerciseId) }
        } else {
            val name = current.exercises.firstOrNull { it.id == workoutExerciseId }?.name ?: return
            _pendingRemoval.value = PendingRemoval(workoutExerciseId, name, setCount)
        }
    }

    fun onRemoveConfirm() {
        val pending = _pendingRemoval.value ?: return
        _pendingRemoval.value = null
        val removedSetIds = session()?.sets?.get(pending.workoutExerciseId).orEmpty().map { it.id }
        if (_keypad.value?.target?.setId in removedSetIds) _keypad.value = null
        scope.launch { workoutRepository.deleteWorkoutExercise(pending.workoutExerciseId) }
    }

    fun onRemoveDismiss() {
        _pendingRemoval.value = null
    }

    fun onWorkoutNotesChange(notes: String) {
        val workoutId = session()?.workout?.id ?: return
        scope.launch { workoutRepository.updateWorkoutNotes(workoutId, notes) }
    }

    fun onExerciseNotesChange(workoutExerciseId: String, notes: String) {
        scope.launch { workoutRepository.updateExerciseNotes(workoutExerciseId, notes) }
    }

    fun onAddSet(workoutExerciseId: String) {
        scope.launch { workoutRepository.addSet(workoutExerciseId) }
    }

    fun onSetTypeChange(setId: String, type: SetType) {
        scope.launch { workoutRepository.updateSetType(setId, type) }
    }

    fun onSetCompletedChange(setId: String, completed: Boolean) {
        scope.launch { workoutRepository.setSetCompleted(setId, completed) }
    }

    fun onDeleteSet(setId: String) {
        if (_keypad.value?.target?.setId == setId) _keypad.value = null
        scope.launch { workoutRepository.deleteSet(setId) }
    }

    /** Opens the keypad on a set's field, starting from what is stored. */
    fun onFieldClick(setId: String, field: SetField) {
        val set = orderedSets().firstOrNull { it.id == setId } ?: return
        _keypad.value = KeypadState(SetFieldRef(setId, field), set.fieldText(field))
    }

    fun onKeypadKey(key: KeypadKey) {
        editKeypad { it.press(key) }
    }

    fun onKeypadStep(up: Boolean) {
        editKeypad { it.stepped(up) }
    }

    /** Moves the keypad on to the next field, or closes it after the last set. */
    fun onKeypadNext() {
        val current = _keypad.value ?: return
        val sets = orderedSets()
        val next = nextField(sets.map { it.id }, current.target)
        val nextSet = next?.let { ref -> sets.firstOrNull { it.id == ref.setId } }
        _keypad.value = if (next != null && nextSet != null) KeypadState(next, nextSet.fieldText(next.field)) else null
    }

    fun onKeypadHide() {
        _keypad.value = null
    }

    /** Closes the keypad and any open sheet or dialog, as when the workout ends. */
    fun close() {
        _keypad.value = null
        _pendingRemoval.value = null
        pickerInput.value = null
    }

    /** Set callbacks for the set table. [onCompletedChange] defaults to just saving the tick. */
    fun setActions(onCompletedChange: (setId: String, completed: Boolean) -> Unit = ::onSetCompletedChange) =
        SetActions(
            onTypeChange = ::onSetTypeChange,
            onCompletedChange = onCompletedChange,
            onDelete = ::onDeleteSet,
            onFieldClick = ::onFieldClick,
        )

    val keypadActions: KeypadActions =
        KeypadActions(onKey = ::onKeypadKey, onStep = ::onKeypadStep, onNext = ::onKeypadNext, onHide = ::onKeypadHide)

    /** Applies an edit and saves the result whenever it is a value the field accepts. */
    private fun editKeypad(edit: (KeypadState) -> KeypadState) {
        val current = _keypad.value ?: return
        val next = edit(current)
        _keypad.value = next
        if (next.text == current.text) return
        val setId = next.target.setId
        when (next.field) {
            SetField.WEIGHT -> parseWeight(next.text).ifValid {
                scope.launch { workoutRepository.updateSetWeight(setId, it) }
            }
            SetField.REPS -> parseReps(next.text).ifValid {
                scope.launch { workoutRepository.updateSetReps(setId, it) }
            }
            SetField.RPE -> parseRpe(next.text).ifValid { scope.launch { workoutRepository.updateSetRpe(setId, it) } }
        }
    }

    /** Every set in the workout, in the order the screen shows them. */
    private fun orderedSets(): List<WorkoutSet> {
        val current = session() ?: return emptyList()
        return current.exercises.flatMap { current.sets[it.id].orEmpty() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private inline fun <T> FieldInput<T>.ifValid(save: (T) -> Unit) {
    if (this is FieldInput.Valid) save(value)
}
