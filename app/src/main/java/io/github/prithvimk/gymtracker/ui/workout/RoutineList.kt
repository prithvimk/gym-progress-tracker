package io.github.prithvimk.gymtracker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.repository.RoutineSummary
import io.github.prithvimk.gymtracker.domain.upNext
import io.github.prithvimk.gymtracker.ui.format.durationText
import io.github.prithvimk.gymtracker.ui.format.relativeDayText

/** What can be done with a routine from the Workout tab. */
class RoutineActions(
    val onStart: (routineId: String) -> Unit,
    val onEdit: (routineId: String?) -> Unit,
    val onDelete: (RoutineSummary) -> Unit,
) {
    companion object {
        val None = RoutineActions({}, {}, {})
    }
}

/**
 * The Workout tab with no workout running: the routine that is up next as a large card, the other
 * routines as rows, and starting an empty workout as a text button at the end.
 */
@Composable
internal fun IdleContent(
    routines: List<RoutineSummary>,
    onStartEmptyClick: () -> Unit,
    actions: RoutineActions,
    modifier: Modifier = Modifier,
) {
    val next = upNext(routines)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.nav_workout),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp).semantics { heading() },
            )
        }
        if (next != null) {
            item(key = "up-next") { UpNextCard(next, actions, Modifier.animateItem()) }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.workout_idle_routines),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                TextButton(onClick = { actions.onEdit(null) }) {
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
        items(routines.filter { it != next }, key = { it.routine.id }) { summary ->
            RoutineRow(summary, actions, Modifier.animateItem())
        }
        item {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                TextButton(onClick = onStartEmptyClick) { Text(stringResource(R.string.workout_start_empty)) }
            }
        }
    }
}

/** The suggested routine: its name, what it holds, and a Start button. Tapping the card edits it. */
@Composable
private fun UpNextCard(summary: RoutineSummary, actions: RoutineActions, modifier: Modifier = Modifier) {
    val name = summary.routine.name
    val id = summary.routine.id
    Card(
        onClick = { actions.onEdit(id) },
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 4.dp)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f).padding(top = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.workout_up_next).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(name, style = MaterialTheme.typography.headlineSmall)
            }
            RoutineMenu(summary, actions)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        ) {
            Text(routineMetaText(summary), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = summary.exerciseNames.joinToString(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Button(onClick = { actions.onStart(id) }, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.workout_start_routine_description, name),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** A routine in the list: its name and what it holds, its menu, and a play button. Tapping it edits it. */
@Composable
private fun RoutineRow(summary: RoutineSummary, actions: RoutineActions, modifier: Modifier = Modifier) {
    val name = summary.routine.name
    val id = summary.routine.id
    Card(onClick = { actions.onEdit(id) }, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = routineMetaText(summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RoutineMenu(summary, actions)
            val description = stringResource(R.string.workout_start_routine_description, name)
            FilledTonalIconButton(
                onClick = { actions.onStart(id) },
                modifier = Modifier.semantics { contentDescription = description },
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
            }
        }
    }
}

/** The ⋮ button with Edit and Delete. */
@Composable
private fun RoutineMenu(summary: RoutineSummary, actions: RoutineActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.workout_routine_options, summary.routine.name),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.workout_routine_edit)) },
                onClick = {
                    open = false
                    actions.onEdit(summary.routine.id)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.workout_routine_delete)) },
                onClick = {
                    open = false
                    actions.onDelete(summary)
                },
            )
        }
    }
}

/** "5 exercises · last done Tuesday · about 58 min", with "not done yet" for a routine never done. */
@Composable
private fun routineMetaText(summary: RoutineSummary): String {
    val count = summary.exerciseCount
    val parts =
        buildList {
            add(
                if (count == 0) {
                    stringResource(R.string.workout_routine_empty)
                } else {
                    pluralStringResource(R.plurals.workout_routine_exercises, count, count)
                },
            )
            val lastDone = summary.lastDoneAt
            add(
                if (lastDone == null) {
                    stringResource(R.string.workout_routine_not_done)
                } else {
                    stringResource(R.string.workout_routine_last_done, relativeDayText(lastDone))
                },
            )
            summary.typicalDurationMillis?.let {
                add(stringResource(R.string.workout_routine_typical_duration, durationText(it)))
            }
        }
    // Each part keeps its words together, so a narrow row wraps between parts, never inside one.
    return parts.joinToString("$NO_BREAK_SPACE· ") { it.replace(' ', NO_BREAK_SPACE) }
}

private const val NO_BREAK_SPACE = '\u00A0'
