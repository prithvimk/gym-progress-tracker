@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.prithvimk.gymtracker.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.github.prithvimk.gymtracker.ui.components.StatTile
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
    onExerciseClick: (exerciseId: String) -> Unit,
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
                onTracksRpeChange = editor::onTracksRpeChange,
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
        onExerciseClick = onExerciseClick,
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
    onExerciseClick: (exerciseId: String) -> Unit = {},
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
                if (editing) {
                    // Filled, like the logger's Finish in the same place. Edits are already saved.
                    Button(
                        onClick = onDone,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(stringResource(R.string.workout_detail_done))
                    }
                } else if (loaded != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.workout_detail_edit))
                    }
                    // Delete sits in the menu, a deliberate tap away from Edit.
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.workout_detail_more)
                            )
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.workout_detail_delete)) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    onDelete()
                                },
                            )
                        }
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
                    // There is no menu here to ask for them, so the notes are always there to edit.
                    alwaysShowNotes = true,
                )

            else -> WorkoutDetailContent(loaded, onExerciseClick, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WorkoutDetailContent(
    state: WorkoutDetailState.Loaded,
    onExerciseClick: (exerciseId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val workout = state.session.workout
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MutedText(formatStartTime(workout.startedAt))
                SummaryTiles(state.summary)
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
            DoneExerciseCard(exercise, notDone, onClick = { onExerciseClick(exercise.entry.exerciseId) })
        }
    }
}

/**
 * One exercise's done sets, with its note and how many planned sets were skipped. An exercise with
 * nothing done is a single "Skipped" line. Its name opens the exercise's history.
 */
@Composable
private fun DoneExerciseCard(exercise: DoneExercise, notDone: Int, onClick: () -> Unit) {
    val entry = exercise.entry
    val skipped = exercise.sets.isEmpty()
    Card(modifier = Modifier.fillMaxWidth()) {
        val openLabel = stringResource(R.string.workout_detail_open_exercise)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = openLabel, onClick = onClick)
                .padding(start = 16.dp, end = 12.dp, top = if (skipped) 12.dp else 16.dp, bottom = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                if (!skipped) {
                    MutedText(
                        stringResource(
                            R.string.exercise_subtitle,
                            stringResource(entry.muscleGroup.labelRes),
                            stringResource(entry.equipment.labelRes),
                        ),
                    )
                }
            }
            if (skipped) MutedText(stringResource(R.string.workout_detail_skipped))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        if (!skipped) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (entry.notes.isNotBlank()) {
                    Text(
                        entry.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                exercise.sets.forEach { HistorySetRow(it) }
                if (notDone > 0) {
                    MutedText(pluralStringResource(R.plurals.workout_detail_not_done, notDone, notDone))
                }
            }
        }
    }
}

/** Duration, volume and sets as tiles, the same as on the finish summary. */
@Composable
private fun SummaryTiles(summary: WorkoutSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(
            durationText(summary.durationMillis),
            stringResource(R.string.workout_summary_duration),
            Modifier.weight(1f)
        )
        StatTile(volumeText(summary.volumeKg), stringResource(R.string.workout_summary_volume), Modifier.weight(1.3f))
        StatTile(summary.setCount.toString(), stringResource(R.string.workout_summary_sets), Modifier.weight(0.7f))
    }
}

@Composable
private fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
