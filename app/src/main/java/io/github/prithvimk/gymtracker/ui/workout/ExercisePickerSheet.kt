@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.prithvimk.gymtracker.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.ui.exercises.labelRes

/** Pick one or more exercises to add to the active workout. They are added in the order they were ticked. */
@Composable
fun ExercisePickerSheet(
    state: ExercisePickerState,
    onQueryChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxHeight(SHEET_HEIGHT_FRACTION)) {
            Text(
                text = stringResource(R.string.picker_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.exercises_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.exercises_clear_search)
                            )
                        }
                    }
                },
                singleLine = true,
            )
            if (state.options.isEmpty()) {
                Text(
                    text = stringResource(R.string.picker_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp).weight(1f),
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.options, key = { it.id }) { exercise ->
                        val checked = exercise.id in state.selectedIds
                        val muscle = stringResource(exercise.muscleGroup.labelRes)
                        val equipment = stringResource(exercise.equipment.labelRes)
                        ListItem(
                            headlineContent = { Text(exercise.name) },
                            supportingContent = { Text(stringResource(R.string.exercise_subtitle, muscle, equipment)) },
                            trailingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.clickable { onToggle(exercise.id) },
                        )
                    }
                }
            }
            Button(
                onClick = onConfirm,
                enabled = state.selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                val count = state.selectedIds.size
                Text(
                    if (count == 0) {
                        stringResource(R.string.picker_add_none)
                    } else {
                        pluralStringResource(R.plurals.picker_add, count, count)
                    },
                )
            }
        }
    }
}

private const val SHEET_HEIGHT_FRACTION = 0.9f
