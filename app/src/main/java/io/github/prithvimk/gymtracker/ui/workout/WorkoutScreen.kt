package io.github.prithvimk.gymtracker.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutExerciseEntry
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.data.repository.RoutineSummary
import io.github.prithvimk.gymtracker.domain.DEFAULT_REST_SECONDS
import io.github.prithvimk.gymtracker.domain.MAX_REST_SECONDS
import io.github.prithvimk.gymtracker.domain.WorkoutSummary
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.domain.model.SetType
import io.github.prithvimk.gymtracker.timer.RestTimerState
import io.github.prithvimk.gymtracker.ui.components.Stepper
import io.github.prithvimk.gymtracker.ui.components.restText
import io.github.prithvimk.gymtracker.ui.exercises.labelRes
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import kotlinx.coroutines.delay

/** The Workout tab. [onEditRoutine] opens the routine editor on a routine, or on a new one for null. */
@Composable
fun WorkoutRoute(
    onEditRoutine: (routineId: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val confirmingDiscard by viewModel.confirmingDiscard.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()
    val editor = viewModel.editor
    val keypad by editor.keypad.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    val restEdit by viewModel.restEdit.collectAsStateWithLifecycle()
    val idleDiscarded by viewModel.idleDiscarded.collectAsStateWithLifecycle()
    // Each time the app comes back, a workout left running by mistake is ended.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    // Asked when a workout starts, since that is when the rest timer will soon want to notify.
    val requestNotifications = rememberNotificationPermissionRequest()
    val restActions = remember(viewModel) { RestActions(viewModel::onRestAdjust, viewModel::onRestSkip) }
    // Ticking a set in the logger also starts the rest timer.
    val setActions = remember(viewModel) { editor.setActions(onCompletedChange = viewModel::onSetCompletedChange) }

    WorkoutScreen(
        state = state,
        onStartClick = {
            requestNotifications()
            viewModel.onStartClick()
        },
        onFinishClick = viewModel::onFinishClick,
        onDiscardClick = viewModel::onDiscardClick,
        onAddExercisesClick = editor::onAddExercisesClick,
        onMoveExercise = editor::onMoveExercise,
        onRemoveExercise = editor::onRemoveExercise,
        onAddSet = editor::onAddSet,
        onWorkoutNotesChange = editor::onWorkoutNotesChange,
        onExerciseNotesChange = editor::onExerciseNotesChange,
        setActions = setActions,
        keypad = keypad,
        keypadActions = editor.keypadActions,
        routines = routines,
        onStartRoutineClick = {
            requestNotifications()
            viewModel.onStartRoutineClick(it)
        },
        onEditRoutine = onEditRoutine,
        rest = rest,
        restActions = restActions,
        onRestEditClick = viewModel::onRestEditClick,
        modifier = modifier,
    )

    SessionEditorDialogs(editor)

    finished?.let { workout ->
        val routineName = workout.savingAsRoutine
        if (routineName == null) {
            WorkoutSummaryDialog(
                summary = workout.summary,
                records = workout.records,
                endedAutomatically = workout.endedAutomatically,
                onSaveAsRoutine = viewModel::onSaveAsRoutineClick,
                onDismiss = viewModel::onSummaryDismiss,
            )
        } else {
            SaveRoutineDialog(
                name = routineName,
                showError = workout.routineNameError,
                onNameChange = viewModel::onRoutineNameChange,
                onSave = viewModel::onSaveRoutineConfirm,
                onDismiss = viewModel::onSaveRoutineDismiss,
            )
        }
    }

    restEdit?.let { edit ->
        AlertDialog(
            onDismissRequest = viewModel::onRestEditDismiss,
            title = { Text(stringResource(R.string.workout_rest_dialog_title, edit.exerciseName)) },
            text = {
                Stepper(
                    label = stringResource(R.string.rest_label),
                    value = restText(edit.seconds),
                    onDecrease = { viewModel.onRestEditStep(up = false) },
                    onIncrease = { viewModel.onRestEditStep(up = true) },
                    canDecrease = edit.seconds > 0,
                    canIncrease = edit.seconds < MAX_REST_SECONDS,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onRestEditSave) {
                    Text(stringResource(R.string.workout_rest_dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onRestEditDismiss) {
                    Text(stringResource(R.string.workout_rest_dialog_cancel))
                }
            },
        )
    }

    if (idleDiscarded) {
        AlertDialog(
            onDismissRequest = viewModel::onIdleDiscardedDismiss,
            title = { Text(stringResource(R.string.workout_idle_discarded_title)) },
            text = { Text(stringResource(R.string.workout_idle_discarded_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::onIdleDiscardedDismiss) {
                    Text(stringResource(R.string.workout_idle_discarded_ok))
                }
            },
        )
    }

    if (confirmingDiscard) {
        AlertDialog(
            onDismissRequest = viewModel::onDiscardDismiss,
            title = { Text(stringResource(R.string.workout_discard_title)) },
            text = { Text(stringResource(R.string.workout_discard_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDiscardConfirm) {
                    Text(stringResource(R.string.workout_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDiscardDismiss) {
                    Text(stringResource(R.string.workout_discard_keep))
                }
            },
        )
    }
}

/** The add-exercises sheet and the confirmation before removing an exercise, while [editor] has them open. */
@Composable
internal fun SessionEditorDialogs(editor: SessionEditor) {
    val picker by editor.picker.collectAsStateWithLifecycle()
    val pendingRemoval by editor.pendingRemoval.collectAsStateWithLifecycle()
    picker?.let { pickerState ->
        ExercisePickerSheet(
            state = pickerState,
            onQueryChange = editor::onPickerQueryChange,
            onToggle = editor::onPickerToggle,
            onConfirm = editor::onPickerConfirm,
            onDismiss = editor::onPickerDismiss,
        )
    }

    pendingRemoval?.let { pending ->
        AlertDialog(
            onDismissRequest = editor::onRemoveDismiss,
            title = { Text(stringResource(R.string.workout_remove_title, pending.name)) },
            text = { Text(pluralStringResource(R.plurals.workout_remove_body, pending.setCount, pending.setCount)) },
            confirmButton = {
                TextButton(onClick = editor::onRemoveConfirm) {
                    Text(stringResource(R.string.workout_remove_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = editor::onRemoveDismiss) {
                    Text(stringResource(R.string.workout_remove_keep))
                }
            },
        )
    }
}

@Composable
private fun WorkoutSummaryDialog(
    summary: WorkoutSummary,
    records: List<ExerciseRecords>,
    endedAutomatically: Boolean,
    onSaveAsRoutine: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workout_summary_title)) },
        text = {
            // Scrolls, since a big session's records can outgrow the dialog.
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                if (endedAutomatically) {
                    Text(
                        text = stringResource(R.string.workout_summary_ended_automatically),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (records.isNotEmpty()) RecordsCelebration(records)
                SummaryRow(stringResource(R.string.workout_summary_duration), formatElapsed(summary.durationMillis))
                SummaryRow(stringResource(R.string.workout_summary_exercises), summary.exerciseCount.toString())
                SummaryRow(stringResource(R.string.workout_summary_sets), summary.setCount.toString())
                SummaryRow(
                    stringResource(R.string.workout_summary_volume),
                    stringResource(R.string.workout_summary_volume_value, formatVolume(summary.volumeKg)),
                )
                if (summary.setCount == 0) {
                    Text(
                        text = stringResource(R.string.workout_summary_no_sets),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.workout_summary_done)) }
        },
        dismissButton = {
            TextButton(onClick = onSaveAsRoutine) { Text(stringResource(R.string.workout_save_as_routine)) }
        },
    )
}

/**
 * The records a workout set, under a trophy, grouped by exercise. The trophy pops in when [celebrate]
 * is set, as on the finish summary.
 */
@Composable
internal fun RecordsCelebration(
    records: List<ExerciseRecords>,
    modifier: Modifier = Modifier,
    celebrate: Boolean = true,
) {
    val trophyScale = remember { Animatable(if (celebrate) 0f else 1f) }
    LaunchedEffect(Unit) {
        trophyScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow))
    }
    val count = records.sumOf { it.records.size }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_trophy),
                contentDescription = null,
                // The container's own content colour, like the text beside it. Plain tertiary is made for
                // surfaces, and on some phones' dynamic dark schemes the container is light enough to hide it.
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(32.dp).scale(trophyScale.value),
            )
            Text(
                text = pluralStringResource(R.plurals.workout_summary_records, count, count),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        records.forEach { exercise ->
            Column {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                exercise.records.forEach { record ->
                    Text(
                        text = recordText(record.type, record.value, record.weightKg, record.reps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}

/** A record in words, e.g. "Most reps at 80 kg: 9". [weightKg] and [reps] are the set holding it. */
@Composable
internal fun recordText(type: PrType, value: Double, weightKg: Double, reps: Int): String = when (type) {
    PrType.WEIGHT -> stringResource(R.string.record_weight, formatSetNumber(value))
    PrType.REPS_AT_WEIGHT ->
        stringResource(R.string.record_reps_at_weight, formatSetNumber(weightKg).ifEmpty { "0" }, reps)
    PrType.E1RM -> stringResource(R.string.record_e1rm, formatVolume(value))
    PrType.VOLUME -> stringResource(R.string.record_volume, formatVolume(value))
}

@Composable
private fun SaveRoutineDialog(
    name: String,
    showError: Boolean,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workout_save_routine_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.workout_save_routine_body))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.workout_save_routine_name)) },
                    isError = showError,
                    supportingText = {
                        if (showError) Text(stringResource(R.string.workout_save_routine_error_blank))
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text(stringResource(R.string.workout_save_routine_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.workout_save_routine_cancel)) }
        },
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    onStartClick: () -> Unit,
    onFinishClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onAddExercisesClick: () -> Unit,
    onMoveExercise: (workoutExerciseId: String, offset: Int) -> Unit,
    onRemoveExercise: (workoutExerciseId: String) -> Unit,
    onAddSet: (workoutExerciseId: String) -> Unit,
    onWorkoutNotesChange: (notes: String) -> Unit,
    onExerciseNotesChange: (workoutExerciseId: String, notes: String) -> Unit,
    setActions: SetActions,
    modifier: Modifier = Modifier,
    keypad: KeypadState? = null,
    keypadActions: KeypadActions = KeypadActions.None,
    routines: List<RoutineSummary> = emptyList(),
    onStartRoutineClick: (routineId: String) -> Unit = {},
    onEditRoutine: (routineId: String?) -> Unit = {},
    rest: RestTimerState? = null,
    restActions: RestActions = RestActions.None,
    onRestEditClick: (workoutExerciseId: String) -> Unit = {},
) {
    when (state) {
        WorkoutUiState.Loading ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        WorkoutUiState.Idle -> IdleContent(onStartClick, routines, onStartRoutineClick, onEditRoutine, modifier)

        is WorkoutUiState.Active ->
            ActiveContent(
                state = state,
                onFinishClick = onFinishClick,
                onDiscardClick = onDiscardClick,
                onAddExercisesClick = onAddExercisesClick,
                onMoveExercise = onMoveExercise,
                onRemoveExercise = onRemoveExercise,
                onAddSet = onAddSet,
                onWorkoutNotesChange = onWorkoutNotesChange,
                onExerciseNotesChange = onExerciseNotesChange,
                setActions = setActions,
                modifier = modifier,
                keypad = keypad,
                keypadActions = keypadActions,
                rest = rest,
                restActions = restActions,
                onRestEditClick = onRestEditClick,
            )
    }
}

@Composable
private fun IdleContent(
    onStartClick: () -> Unit,
    routines: List<RoutineSummary>,
    onStartRoutineClick: (routineId: String) -> Unit,
    onEditRoutine: (routineId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.workout_idle_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = stringResource(R.string.workout_idle_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onStartClick) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text(stringResource(R.string.workout_start_empty), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.workout_idle_routines),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onEditRoutine(null) }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.workout_new_routine), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        if (routines.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.workout_idle_no_routines),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
        }
        items(routines, key = { it.routine.id }) { summary ->
            RoutineCard(
                summary = summary,
                onStart = { onStartRoutineClick(summary.routine.id) },
                onEdit = { onEditRoutine(summary.routine.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun RoutineCard(
    summary: RoutineSummary,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = summary.routine.name
    Card(onClick = onEdit, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = summary.exerciseNames.joinToString().ifEmpty {
                        stringResource(R.string.workout_routine_empty)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val description = stringResource(R.string.workout_start_routine_description, name)
            FilledTonalButton(
                onClick = onStart,
                modifier = Modifier.padding(start = 8.dp).semantics { contentDescription = description },
            ) {
                Text(stringResource(R.string.workout_start_routine))
            }
        }
    }
}

@Composable
private fun ActiveContent(
    state: WorkoutUiState.Active,
    onFinishClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onAddExercisesClick: () -> Unit,
    onMoveExercise: (workoutExerciseId: String, offset: Int) -> Unit,
    onRemoveExercise: (workoutExerciseId: String) -> Unit,
    onAddSet: (workoutExerciseId: String) -> Unit,
    onWorkoutNotesChange: (notes: String) -> Unit,
    onExerciseNotesChange: (workoutExerciseId: String, notes: String) -> Unit,
    setActions: SetActions,
    modifier: Modifier = Modifier,
    keypad: KeypadState? = null,
    keypadActions: KeypadActions = KeypadActions.None,
    rest: RestTimerState? = null,
    restActions: RestActions = RestActions.None,
    onRestEditClick: (workoutExerciseId: String) -> Unit = {},
) {
    val startedAt = state.workout.startedAt
    // Elapsed time is derived from startedAt, so it stays right across process death.
    val elapsed by produceState(System.currentTimeMillis() - startedAt, startedAt) {
        while (true) {
            value = System.currentTimeMillis() - startedAt
            delay(TICK_MS)
        }
    }
    SessionEditorContent(
        state = state,
        actions =
        SessionEditActions(
            onAddExercisesClick = onAddExercisesClick,
            onMoveExercise = onMoveExercise,
            onRemoveExercise = onRemoveExercise,
            onAddSet = onAddSet,
            onWorkoutNotesChange = onWorkoutNotesChange,
            onExerciseNotesChange = onExerciseNotesChange,
            setActions = setActions,
            keypadActions = keypadActions,
            onRestEditClick = onRestEditClick,
        ),
        keypad = keypad,
        modifier = modifier,
        header = {
            Text(
                text = state.routineName ?: stringResource(R.string.workout_active_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = formatElapsed(elapsed),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        },
        // Stays up over the keypad too, since the next set's numbers are often typed while resting.
        aboveKeypad = { if (rest != null) RestBar(rest = rest, actions = restActions) },
        footer = {
            OutlinedButton(onClick = onDiscardClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.workout_discard))
            }
            Button(onClick = onFinishClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.workout_finish))
            }
        },
    )
}

/**
 * The edits [SessionEditorContent] passes on. [onRestEditClick] is null where rest times don't apply,
 * as in a finished workout, and then rest times are neither shown nor offered.
 */
@Immutable
internal class SessionEditActions(
    val onAddExercisesClick: () -> Unit,
    val onMoveExercise: (workoutExerciseId: String, offset: Int) -> Unit,
    val onRemoveExercise: (workoutExerciseId: String) -> Unit,
    val onAddSet: (workoutExerciseId: String) -> Unit,
    val onWorkoutNotesChange: (notes: String) -> Unit,
    val onExerciseNotesChange: (workoutExerciseId: String, notes: String) -> Unit,
    val setActions: SetActions,
    val keypadActions: KeypadActions,
    val onRestEditClick: ((workoutExerciseId: String) -> Unit)?,
)

/**
 * A workout's notes and exercises with their set tables, as the logger edits them, under [header].
 * The keypad comes up over [footer]'s row of buttons while it is typing into a set; [aboveKeypad]
 * sits above both. Both are put away while the system keyboard is up for a note.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SessionEditorContent(
    state: WorkoutUiState.Active,
    actions: SessionEditActions,
    keypad: KeypadState?,
    header: @Composable ColumnScope.() -> Unit,
    footer: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    aboveKeypad: @Composable () -> Unit = {},
) {
    val keypadActions = actions.keypadActions
    // Compose keeps a field focused until told otherwise, so the cursor would linger after the
    // keyboard is put away or the user taps elsewhere. Both of those end editing here.
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        // The system keyboard is up for a note, which takes over from the keypad.
        if (imeVisible) keypadActions.onHide() else focusManager.clearFocus()
    }
    // A keypad on a set that has since gone (deleted elsewhere, or its exercise removed) is closed.
    val openKeypad = keypad?.takeIf { k -> state.sets.values.any { sets -> sets.any { it.id == k.target.setId } } }
    BackHandler(enabled = openKeypad != null, onBack = keypadActions.onHide)

    val endEditing = {
        focusManager.clearFocus()
        keypadActions.onHide()
    }

    // Fields and buttons handle their own taps; a tap on anything else lands here.
    Column(modifier = modifier.fillMaxSize().pointerInput(keypadActions) { detectTapGestures { endEditing() } }) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp), content = header)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = WORKOUT_NOTES_KEY) {
                NotesField(
                    key = state.workout.id,
                    initial = state.workout.notes,
                    label = stringResource(R.string.workout_notes),
                    placeholder = stringResource(R.string.workout_notes_placeholder),
                    onChange = actions.onWorkoutNotesChange,
                )
            }
            if (state.exercises.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.workout_no_exercises),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    )
                }
            }
            itemsIndexed(state.exercises, key = { _, entry -> entry.id }) { index, entry ->
                val onRestEditClick = actions.onRestEditClick
                ExerciseCard(
                    entry = entry,
                    sets = state.sets[entry.id].orEmpty(),
                    previousSets = state.previousSets[entry.exerciseId].orEmpty(),
                    badges = state.badges,
                    keypad = openKeypad,
                    setActions = actions.setActions,
                    onAddSet = { actions.onAddSet(entry.id) },
                    onNotesChange = { actions.onExerciseNotesChange(entry.id, it) },
                    canMoveUp = index > 0,
                    canMoveDown = index < state.exercises.lastIndex,
                    onMove = { offset -> actions.onMoveExercise(entry.id, offset) },
                    onRemove = { actions.onRemoveExercise(entry.id) },
                    onRestEditClick = onRestEditClick?.let { { it(entry.id) } },
                    modifier = Modifier.animateItem(),
                )
            }
            item {
                FilledTonalButton(onClick = actions.onAddExercisesClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.workout_add_exercises), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        if (!imeVisible) aboveKeypad()
        if (openKeypad != null) {
            Keypad(field = openKeypad.field, actions = keypadActions)
        } else if (!imeVisible) {
            // Hidden while typing, so the keyboard leaves as much of the list visible as it can.
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                content = footer,
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    entry: WorkoutExerciseEntry,
    sets: List<WorkoutSet>,
    previousSets: List<WorkoutSet>,
    badges: Map<String, List<PrType>>,
    keypad: KeypadState?,
    setActions: SetActions,
    onAddSet: () -> Unit,
    onNotesChange: (notes: String) -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (offset: Int) -> Unit,
    onRemove: () -> Unit,
    onRestEditClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    // A note shows once it has text, or as soon as one is asked for; asking focuses it.
    var showNote by rememberSaveable(entry.id) { mutableStateOf(entry.notes.isNotEmpty()) }
    var focusNote by remember { mutableStateOf(false) }
    val muscle = stringResource(entry.muscleGroup.labelRes)
    val equipment = stringResource(entry.equipment.labelRes)
    Card(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.exercise_subtitle, muscle, equipment),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    targetsText(entry, showRest = onRestEditClick != null)?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.workout_exercise_options, entry.name),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (!showNote) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.workout_add_note)) },
                                onClick = {
                                    menuOpen = false
                                    showNote = true
                                    focusNote = true
                                },
                            )
                        }
                        if (onRestEditClick != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.workout_rest_time)) },
                                onClick = {
                                    menuOpen = false
                                    onRestEditClick()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workout_move_up)) },
                            enabled = canMoveUp,
                            onClick = {
                                menuOpen = false
                                onMove(-1)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workout_move_down)) },
                            enabled = canMoveDown,
                            onClick = {
                                menuOpen = false
                                onMove(1)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.workout_remove_exercise)) },
                            onClick = {
                                menuOpen = false
                                onRemove()
                            },
                        )
                    }
                }
            }
            if (showNote) {
                NotesField(
                    key = entry.id,
                    initial = entry.notes,
                    label = stringResource(R.string.workout_exercise_note),
                    placeholder = stringResource(R.string.workout_exercise_note_placeholder),
                    onChange = onNotesChange,
                    requestFocus = focusNote,
                    // Clear of the set table's header, so the note reads as its own block.
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
            SetTable(
                sets = sets,
                previousSets = previousSets,
                keypad = keypad,
                actions = setActions,
                onAddSet = onAddSet,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
                badges = badges,
            )
        }
    }
}

/**
 * The exercise's rep range from its routine, if any, and its rest time when [showRest]: "8–12 reps ·
 * Rest 2:00". Null when there is neither.
 */
@Composable
private fun targetsText(entry: WorkoutExerciseEntry, showRest: Boolean): String? {
    val rest =
        if (showRest) {
            stringResource(R.string.workout_exercise_rest, restText(entry.restSeconds ?: DEFAULT_REST_SECONDS))
        } else {
            null
        }
    val min = entry.repMin
    val max = entry.repMax
    if (min == null || max == null) return rest
    val targets = stringResource(R.string.workout_exercise_targets, min, max)
    return if (rest == null) targets else stringResource(R.string.exercise_subtitle, targets, rest)
}

/**
 * A free-text note that keeps what you type and saves every change. It reads [initial] once per
 * [key], so saves coming back from the database never move the cursor.
 */
@Composable
private fun NotesField(
    key: String,
    initial: String,
    label: String,
    placeholder: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    requestFocus: Boolean = false,
) {
    var text by rememberSaveable(key) { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) focusRequester.requestFocus()
    }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onChange(it)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        maxLines = NOTES_MAX_LINES,
        modifier = modifier.fillMaxWidth().focusRequester(focusRequester),
    )
}

private const val TICK_MS = 1_000L
private const val NOTES_MAX_LINES = 5
private const val WORKOUT_NOTES_KEY = "workout-notes"

@Preview(showBackground = true)
@Composable
private fun IdlePreview() {
    GymTrackerTheme(dynamicColor = false) {
        WorkoutScreen(
            state = WorkoutUiState.Idle,
            onStartClick = {},
            onFinishClick = {},
            onDiscardClick = {},
            onAddExercisesClick = {},
            onMoveExercise = { _, _ -> },
            onRemoveExercise = {},
            onAddSet = {},
            onWorkoutNotesChange = {},
            onExerciseNotesChange = { _, _ -> },
            setActions = SetActions.None,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivePreview() {
    val workout =
        Workout(
            id = "preview",
            routineId = null,
            startedAt = System.currentTimeMillis() - 754_000L,
            endedAt = null,
            notes = "",
            createdAt = 0L,
            updatedAt = 0L,
        )
    val exercises =
        listOf(
            WorkoutExerciseEntry(
                "a",
                "bench",
                0,
                "Barbell Bench Press",
                MuscleGroup.CHEST,
                Equipment.BARBELL,
                "Seat 4",
                repMin = 6,
                repMax = 8,
                restSeconds = 180,
            ),
            WorkoutExerciseEntry(
                "b",
                "ohp",
                1,
                "Overhead Press",
                MuscleGroup.SHOULDERS,
                Equipment.BARBELL,
                "",
                repMin = null,
                repMax = null,
                restSeconds = null,
            ),
        )
    val sets =
        mapOf(
            "a" to
                listOf(
                    previewSet("s1", "a", 0, 40.0, 10, SetType.WARMUP, completedAt = 1L),
                    previewSet("s2", "a", 1, 80.0, 8, SetType.WORKING, completedAt = 2L),
                    previewSet("s3", "a", 2, 80.0, 8, SetType.WORKING, completedAt = null),
                ),
        )
    GymTrackerTheme(dynamicColor = false) {
        WorkoutScreen(
            state = WorkoutUiState.Active(workout, exercises, sets, badges = mapOf("s2" to listOf(PrType.WEIGHT))),
            onStartClick = {},
            onFinishClick = {},
            onDiscardClick = {},
            onAddExercisesClick = {},
            onMoveExercise = { _, _ -> },
            onRemoveExercise = {},
            onAddSet = {},
            onWorkoutNotesChange = {},
            onExerciseNotesChange = { _, _ -> },
            setActions = SetActions.None,
        )
    }
}

private fun previewSet(
    id: String,
    workoutExerciseId: String,
    position: Int,
    weightKg: Double,
    reps: Int,
    type: SetType,
    completedAt: Long?,
) = WorkoutSet(id, workoutExerciseId, position, weightKg, reps, null, type, completedAt, 0L, 0L)
