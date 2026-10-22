@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.prithvimk.gymtracker.ui.routines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.domain.ExerciseTargets
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.ui.components.Stepper
import io.github.prithvimk.gymtracker.ui.components.restText
import io.github.prithvimk.gymtracker.ui.exercises.labelRes
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import io.github.prithvimk.gymtracker.ui.workout.ExercisePickerSheet

@Composable
fun RoutineEditorRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutineEditorViewModel = viewModel(factory = RoutineEditorViewModel.factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker by viewModel.picker.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }
    BackHandler(enabled = !state.done, onBack = viewModel::onBack)

    RoutineEditorScreen(
        state = state,
        actions =
        RoutineEditorActions(
            onBack = viewModel::onBack,
            onSave = viewModel::onSave,
            onDelete = viewModel::onDeleteClick,
            onNameChange = viewModel::onNameChange,
            onNotesChange = viewModel::onNotesChange,
            onStep = viewModel::onStep,
            onMove = viewModel::onMove,
            onRemove = viewModel::onRemove,
            onAddExercises = viewModel::onAddExercisesClick,
        ),
        modifier = modifier,
    )

    picker?.let { pickerState ->
        ExercisePickerSheet(
            state = pickerState,
            onQueryChange = viewModel::onPickerQueryChange,
            onToggle = viewModel::onPickerToggle,
            onConfirm = viewModel::onPickerConfirm,
            onDismiss = viewModel::onPickerDismiss,
        )
    }

    if (state.confirmingDiscard) {
        AlertDialog(
            onDismissRequest = viewModel::onDiscardDismiss,
            title = { Text(stringResource(R.string.routine_discard_title)) },
            text = { Text(stringResource(R.string.routine_discard_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDiscardConfirm) {
                    Text(stringResource(R.string.routine_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDiscardDismiss) {
                    Text(stringResource(R.string.routine_discard_keep))
                }
            },
        )
    }

    if (state.confirmingDelete) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text(stringResource(R.string.routine_delete_title, state.name)) },
            text = { Text(stringResource(R.string.routine_delete_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirm) {
                    Text(stringResource(R.string.routine_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismiss) {
                    Text(stringResource(R.string.routine_delete_keep))
                }
            },
        )
    }
}

/** What the routine editor can do. Exercise callbacks take the exercise's draft key. */
class RoutineEditorActions(
    val onBack: () -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
    val onNameChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onStep: (key: String, field: TargetField, up: Boolean) -> Unit,
    val onMove: (key: String, offset: Int) -> Unit,
    val onRemove: (key: String) -> Unit,
    val onAddExercises: () -> Unit,
) {
    companion object {
        val None = RoutineEditorActions({}, {}, {}, {}, {}, { _, _, _ -> }, { _, _ -> }, {}, {})
    }
}

@Composable
fun RoutineEditorScreen(state: RoutineEditorState, actions: RoutineEditorActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(stringResource(if (state.isNew) R.string.routine_title_new else R.string.routine_title_edit))
            },
            navigationIcon = {
                IconButton(onClick = actions.onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.routine_back)
                    )
                }
            },
            actions = {
                if (!state.isNew) {
                    IconButton(onClick = actions.onDelete, enabled = !state.saving) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.routine_delete))
                    }
                }
                TextButton(onClick = actions.onSave, enabled = !state.saving && !state.loading) {
                    Text(stringResource(R.string.routine_save))
                }
            },
        )
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = NAME_KEY) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = actions.onNameChange,
                    label = { Text(stringResource(R.string.routine_name)) },
                    placeholder = { Text(stringResource(R.string.routine_name_placeholder)) },
                    isError = state.nameError,
                    supportingText = {
                        if (state.nameError) Text(stringResource(R.string.routine_error_blank))
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = NOTES_KEY) {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = actions.onNotesChange,
                    label = { Text(stringResource(R.string.routine_notes)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = NOTES_MAX_LINES,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.exercises.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.routine_no_exercises),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    )
                }
            }
            itemsIndexed(state.exercises, key = { _, exercise -> exercise.key }) { index, exercise ->
                RoutineExerciseCard(
                    exercise = exercise,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.exercises.lastIndex,
                    actions = actions,
                    modifier = Modifier.animateItem(),
                )
            }
            item {
                FilledTonalButton(onClick = actions.onAddExercises, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.routine_add_exercises), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun RoutineExerciseCard(
    exercise: DraftExercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    actions: RoutineEditorActions,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val muscle = stringResource(exercise.muscleGroup.labelRes)
    val equipment = stringResource(exercise.equipment.labelRes)
    val targets = exercise.targets
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.exercise_subtitle, muscle, equipment),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.workout_exercise_options, exercise.name),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workout_move_up)) },
                            enabled = canMoveUp,
                            onClick = {
                                menuOpen = false
                                actions.onMove(exercise.key, -1)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workout_move_down)) },
                            enabled = canMoveDown,
                            onClick = {
                                menuOpen = false
                                actions.onMove(exercise.key, 1)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workout_remove_exercise)) },
                            onClick = {
                                menuOpen = false
                                actions.onRemove(exercise.key)
                            },
                        )
                    }
                }
            }
            TargetStepper(
                exercise,
                TargetField.SETS,
                stringResource(R.string.routine_sets),
                "${targets.targetSets}",
                actions
            )
            TargetStepper(
                exercise,
                TargetField.REP_MIN,
                stringResource(R.string.routine_rep_min),
                "${targets.repMin}",
                actions
            )
            TargetStepper(
                exercise,
                TargetField.REP_MAX,
                stringResource(R.string.routine_rep_max),
                "${targets.repMax}",
                actions
            )
            TargetStepper(
                exercise,
                TargetField.REST,
                stringResource(R.string.rest_label),
                restText(targets.restSeconds),
                actions,
            )
        }
    }
}

@Composable
private fun TargetStepper(
    exercise: DraftExercise,
    field: TargetField,
    label: String,
    value: String,
    actions: RoutineEditorActions,
) {
    Stepper(
        label = label,
        value = value,
        onDecrease = { actions.onStep(exercise.key, field, false) },
        onIncrease = { actions.onStep(exercise.key, field, true) },
        canDecrease = exercise.targets.canStep(field, up = false),
        canIncrease = exercise.targets.canStep(field, up = true),
    )
}

private const val NAME_KEY = "name"
private const val NOTES_KEY = "notes"
private const val NOTES_MAX_LINES = 4

@Preview(showBackground = true)
@Composable
private fun RoutineEditorPreview() {
    val state =
        RoutineEditorState(
            isNew = false,
            loading = false,
            name = "Push",
            exercises =
            listOf(
                DraftExercise(
                    key = "a",
                    id = "a",
                    exerciseId = "bench",
                    name = "Barbell Bench Press",
                    muscleGroup = MuscleGroup.CHEST,
                    equipment = Equipment.BARBELL,
                    targets = ExerciseTargets(3, 6, 8, 180),
                ),
            ),
        )
    GymTrackerTheme(dynamicColor = false) {
        RoutineEditorScreen(state = state, actions = RoutineEditorActions.None)
    }
}
