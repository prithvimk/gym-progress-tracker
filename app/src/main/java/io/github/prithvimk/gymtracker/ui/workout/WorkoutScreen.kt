package io.github.prithvimk.gymtracker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutExerciseEntry
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.domain.model.SetType
import io.github.prithvimk.gymtracker.ui.exercises.labelRes
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import kotlinx.coroutines.delay

@Composable
fun WorkoutRoute(
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val confirmingDiscard by viewModel.confirmingDiscard.collectAsStateWithLifecycle()
    val picker by viewModel.picker.collectAsStateWithLifecycle()
    val pendingRemoval by viewModel.pendingRemoval.collectAsStateWithLifecycle()
    val setActions =
        remember(viewModel) {
            SetActions(
                onWeightChange = viewModel::onSetWeightChange,
                onRepsChange = viewModel::onSetRepsChange,
                onRpeChange = viewModel::onSetRpeChange,
                onTypeChange = viewModel::onSetTypeChange,
                onCompletedChange = viewModel::onSetCompletedChange,
                onDelete = viewModel::onDeleteSet,
            )
        }

    WorkoutScreen(
        state = state,
        onStartClick = viewModel::onStartClick,
        onFinishClick = viewModel::onFinishClick,
        onDiscardClick = viewModel::onDiscardClick,
        onAddExercisesClick = viewModel::onAddExercisesClick,
        onMoveExercise = viewModel::onMoveExercise,
        onRemoveExercise = viewModel::onRemoveExercise,
        onAddSet = viewModel::onAddSet,
        setActions = setActions,
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

    pendingRemoval?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::onRemoveDismiss,
            title = { Text(stringResource(R.string.workout_remove_title, pending.name)) },
            text = { Text(pluralStringResource(R.plurals.workout_remove_body, pending.setCount, pending.setCount)) },
            confirmButton = {
                TextButton(onClick = viewModel::onRemoveConfirm) {
                    Text(stringResource(R.string.workout_remove_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onRemoveDismiss) {
                    Text(stringResource(R.string.workout_remove_keep))
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
    setActions: SetActions,
    modifier: Modifier = Modifier,
) {
    when (state) {
        WorkoutUiState.Loading ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        WorkoutUiState.Idle -> IdleContent(onStartClick, modifier)

        is WorkoutUiState.Active ->
            ActiveContent(
                state = state,
                onFinishClick = onFinishClick,
                onDiscardClick = onDiscardClick,
                onAddExercisesClick = onAddExercisesClick,
                onMoveExercise = onMoveExercise,
                onRemoveExercise = onRemoveExercise,
                onAddSet = onAddSet,
                setActions = setActions,
                modifier = modifier,
            )
    }
}

@Composable
private fun IdleContent(onStartClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
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

@Composable
private fun ActiveContent(
    state: WorkoutUiState.Active,
    onFinishClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onAddExercisesClick: () -> Unit,
    onMoveExercise: (workoutExerciseId: String, offset: Int) -> Unit,
    onRemoveExercise: (workoutExerciseId: String) -> Unit,
    onAddSet: (workoutExerciseId: String) -> Unit,
    setActions: SetActions,
    modifier: Modifier = Modifier,
) {
    val startedAt = state.workout.startedAt
    // Elapsed time is derived from startedAt, so it stays right across process death.
    val elapsed by produceState(System.currentTimeMillis() - startedAt, startedAt) {
        while (true) {
            value = System.currentTimeMillis() - startedAt
            delay(TICK_MS)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text(
                text = stringResource(R.string.workout_active_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = formatElapsed(elapsed),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
                ExerciseCard(
                    entry = entry,
                    sets = state.sets[entry.id].orEmpty(),
                    setActions = setActions,
                    onAddSet = { onAddSet(entry.id) },
                    canMoveUp = index > 0,
                    canMoveDown = index < state.exercises.lastIndex,
                    onMove = { offset -> onMoveExercise(entry.id, offset) },
                    onRemove = { onRemoveExercise(entry.id) },
                    modifier = Modifier.animateItem(),
                )
            }
            item {
                FilledTonalButton(onClick = onAddExercisesClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.workout_add_exercises), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            OutlinedButton(onClick = onDiscardClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.workout_discard))
            }
            Button(onClick = onFinishClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.workout_finish))
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    entry: WorkoutExerciseEntry,
    sets: List<WorkoutSet>,
    setActions: SetActions,
    onAddSet: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (offset: Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
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
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.workout_exercise_options, entry.name),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
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
            SetTable(
                sets = sets,
                actions = setActions,
                onAddSet = onAddSet,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            )
        }
    }
}

private const val TICK_MS = 1_000L

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
            WorkoutExerciseEntry("a", "bench", 0, "Barbell Bench Press", MuscleGroup.CHEST, Equipment.BARBELL),
            WorkoutExerciseEntry("b", "ohp", 1, "Overhead Press", MuscleGroup.SHOULDERS, Equipment.BARBELL),
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
            state = WorkoutUiState.Active(workout, exercises, sets),
            onStartClick = {},
            onFinishClick = {},
            onDiscardClick = {},
            onAddExercisesClick = {},
            onMoveExercise = { _, _ -> },
            onRemoveExercise = {},
            onAddSet = {},
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
