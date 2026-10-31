@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.prithvimk.gymtracker.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.domain.WorkoutSummary
import io.github.prithvimk.gymtracker.ui.exercises.HistorySetRow
import io.github.prithvimk.gymtracker.ui.exercises.labelRes
import io.github.prithvimk.gymtracker.ui.format.durationText
import io.github.prithvimk.gymtracker.ui.format.volumeText
import io.github.prithvimk.gymtracker.ui.workout.KeypadState
import io.github.prithvimk.gymtracker.ui.workout.RecordsCelebration
import io.github.prithvimk.gymtracker.ui.workout.SessionEditActions
import io.github.prithvimk.gymtracker.ui.workout.SessionEditorContent
import io.github.prithvimk.gymtracker.ui.workout.SessionEditorDialogs

/** A finished workout, opened from History. */
@Composable
fun WorkoutDetailRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkoutDetailViewModel = viewModel(factory = WorkoutDetailViewModel.factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val confirmingDelete by viewModel.confirmingDelete.collectAsStateWithLifecycle()
    val editor = viewModel.editor
    val keypad by editor.keypad.collectAsStateWithLifecycle()
    val editActions =
        remember(viewModel) {
            SessionEditActions(
                onAddExercisesClick = editor::onAddExercisesClick,
                onMoveExercise = editor::onMoveExercise,
                onRemoveExercise = editor::onRemoveExercise,
                onAddSet = editor::onAddSet,
                onWorkoutNotesChange = editor::onWorkoutNotesChange,
                onExerciseNotesChange = editor::onExerciseNotesChange,
                setActions = editor.setActions(),
                keypadActions = editor.keypadActions,
                // Rest times only matter while a workout is running.
                onRestEditClick = null,
            )
        }

    LaunchedEffect(state == WorkoutDetailState.Missing) {
        if (state == WorkoutDetailState.Missing) onBack()
    }

    WorkoutDetailScreen(
        state = state,
        onBack = onBack,
        onEdit = viewModel::onEditClick,
        onDone = viewModel::onDoneClick,
        onDelete = viewModel::onDeleteClick,
        editActions = editActions,
        keypad = keypad,
        modifier = modifier,
    )

    SessionEditorDialogs(editor)

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text(stringResource(R.string.workout_detail_delete_title)) },
            text = { Text(stringResource(R.string.workout_detail_delete_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirm) {
                    Text(stringResource(R.string.workout_detail_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDeleteDismiss) {
                    Text(stringResource(R.string.workout_detail_delete_keep))
                }
            },
        )
    }
}

@Composable
internal fun WorkoutDetailScreen(
    state: WorkoutDetailState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
    editActions: SessionEditActions,
    modifier: Modifier = Modifier,
    keypad: KeypadState? = null,
) {
    val loaded = state as? WorkoutDetailState.Loaded
    val editing = loaded?.editing == true
    // Back leaves the editor before it leaves the screen. Edits are already saved.
    BackHandler(enabled = editing, onBack = onDone)
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = loaded?.let { it.session.routineName ?: stringResource(R.string.history_workout_untitled) }
                        .orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = if (editing) onDone else onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.workout_detail_back),
                    )
                }
            },
            actions = {
                if (loaded != null && !editing) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.workout_detail_edit))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.workout_detail_delete))
                    }
                }
            },
        )
        when {
            loaded == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            editing ->
                SessionEditorContent(
                    state = loaded.session,
                    actions = editActions,
                    keypad = keypad,
                    modifier = Modifier.weight(1f),
                    header = {
                        MutedText(formatStartTime(loaded.session.workout.startedAt))
                        MutedText(stringResource(R.string.workout_detail_edit_hint))
                    },
                    footer = {
                        Button(onClick = onDone, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.workout_detail_done))
                        }
                    },
                )

            else -> WorkoutDetailContent(loaded, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WorkoutDetailContent(state: WorkoutDetailState.Loaded, modifier: Modifier = Modifier,) {
    val workout = state.session.workout
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MutedText(formatStartTime(workout.startedAt))
                Text(text = statsText(state.summary), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (state.records.isNotEmpty()) {
            item { RecordsCelebration(state.records, Modifier.padding(top = 8.dp), celebrate = false) }
        }
        if (workout.notes.isNotBlank()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.workout_detail_notes), style = MaterialTheme.typography.titleSmall)
                        Text(workout.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        items(state.exercises, key = { it.entry.id }) { exercise ->
            val notDone = state.session.sets[exercise.entry.id].orEmpty().size - exercise.sets.size
            DoneExerciseCard(exercise, notDone)
        }
    }
}

/** One exercise's done sets, with its note and how many sets were left undone. */
@Composable
private fun DoneExerciseCard(exercise: DoneExercise, notDone: Int) {
    val entry = exercise.entry
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.name, style = MaterialTheme.typography.titleMedium)
            MutedText(
                stringResource(
                    R.string.exercise_subtitle,
                    stringResource(entry.muscleGroup.labelRes),
                    stringResource(entry.equipment.labelRes),
                ),
            )
            if (entry.notes.isNotBlank()) Text(entry.notes, style = MaterialTheme.typography.bodyMedium)
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (exercise.sets.isEmpty()) MutedText(stringResource(R.string.workout_detail_no_sets))
                exercise.sets.forEach { HistorySetRow(it) }
                if (notDone > 0) MutedText(pluralStringResource(R.plurals.workout_detail_not_done, notDone, notDone))
            }
        }
    }
}

/** "1 h 2 min · 18 sets · 8,420 kg", the same as the workout's card in History. */
@Composable
private fun statsText(summary: WorkoutSummary): String = stringResource(
    R.string.history_workout_stats,
    durationText(summary.durationMillis),
    pluralStringResource(R.plurals.history_workout_sets, summary.setCount, summary.setCount),
    volumeText(summary.volumeKg),
)

@Composable
private fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
