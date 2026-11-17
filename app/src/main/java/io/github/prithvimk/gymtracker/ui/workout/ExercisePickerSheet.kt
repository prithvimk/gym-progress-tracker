@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.prithvimk.gymtracker.ui.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.ui.exercises.labelRes
import io.github.prithvimk.gymtracker.ui.theme.tabular

/** What the add-exercises sheet does. */
class PickerActions(
    val onQueryChange: (String) -> Unit,
    val onChipSelect: (PickerChip) -> Unit,
    val onToggle: (String) -> Unit,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
) {
    companion object {
        val None = PickerActions({}, {}, {}, {}, {})
    }
}

/** Pick one or more exercises to add. They are added in the order they were ticked, which the numbers show. */
@Composable
fun ExercisePickerSheet(state: ExercisePickerState, actions: PickerActions) {
    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ExercisePickerContent(state, actions, Modifier.fillMaxHeight(SHEET_HEIGHT_FRACTION))
    }
}

/**
 * The sheet's content: search, chips with Recent first, the list with a number on each pick, and a
 * tray of the picks in order above the Add button.
 */
@Composable
internal fun ExercisePickerContent(state: ExercisePickerState, actions: PickerActions, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.picker_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        PickerSearch(state.query, actions.onQueryChange)
        PickerChips(state, actions.onChipSelect)
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
                    val order = state.selectedIds.indexOf(exercise.id)
                    PickerRow(exercise, order, onToggle = { actions.onToggle(exercise.id) })
                }
            }
        }
        if (state.picks.isNotEmpty()) PicksTray(state.picks, actions.onToggle)
        Button(
            onClick = actions.onConfirm,
            enabled = state.selectedIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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

@Composable
private fun PickerSearch(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    Surface(
        shape = SearchBarDefaults.inputFieldShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SearchBarDefaults.InputField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { focusManager.clearFocus() },
            expanded = false,
            onExpandedChange = {},
            placeholder = { Text(stringResource(R.string.exercises_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.exercises_clear_search))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Recent (when there are recent exercises), All, then a chip per muscle group. */
@Composable
private fun PickerChips(state: ExercisePickerState, onSelect: (PickerChip) -> Unit) {
    val shown = state.shownChip
    val chips =
        buildList {
            if (state.hasRecent) add(PickerChip.Recent)
            add(PickerChip.All)
            MuscleGroup.entries.forEach { add(PickerChip.Muscle(it)) }
        }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 4.dp),
    ) {
        items(chips) { chip ->
            FilterChip(
                selected = chip == shown,
                onClick = { onSelect(chip) },
                label = {
                    Text(
                        when (chip) {
                            PickerChip.Recent -> stringResource(R.string.picker_recent)
                            PickerChip.All -> stringResource(R.string.exercises_filter_all)
                            is PickerChip.Muscle -> stringResource(chip.group.labelRes)
                        },
                    )
                },
            )
        }
    }
}

/** An exercise to pick. [order] is its place among the picks, from 0, or -1 if it isn't picked. */
@Composable
private fun PickerRow(exercise: Exercise, order: Int, onToggle: () -> Unit) {
    val picked = order >= 0
    val muscle = stringResource(exercise.muscleGroup.labelRes)
    val equipment = stringResource(exercise.equipment.labelRes)
    ListItem(
        headlineContent = { Text(exercise.name) },
        supportingContent = { Text(stringResource(R.string.exercise_subtitle, muscle, equipment)) },
        trailingContent = { PickMark(order) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.toggleable(value = picked, role = Role.Checkbox, onValueChange = { onToggle() }),
    )
}

/** A filled disc with the pick's number in tick order, or an empty ring when not picked. */
@Composable
private fun PickMark(order: Int) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier =
        if (order >= 0) {
            Modifier.size(28.dp).background(colors.primary, CircleShape)
        } else {
            Modifier.size(28.dp).border(BorderStroke(2.dp, colors.outline), CircleShape)
        },
    ) {
        if (order >= 0) {
            Text(
                text = (order + 1).toString(),
                style = MaterialTheme.typography.labelLarge.tabular(),
                color = colors.onPrimary,
            )
        }
    }
}

/** The picks in the order they'll be added, each removable. */
@Composable
private fun PicksTray(picks: List<Exercise>, onRemove: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        itemsIndexed(picks, key = { _, exercise -> exercise.id }) { index, exercise ->
            val remove = stringResource(R.string.picker_remove, exercise.name)
            InputChip(
                selected = true,
                onClick = { onRemove(exercise.id) },
                label = { Text(stringResource(R.string.picker_tray_item, index + 1, exercise.name)) },
                trailingIcon = {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = remove,
                        modifier = Modifier.size(InputChipDefaults.IconSize)
                    )
                },
            )
        }
    }
}

private const val SHEET_HEIGHT_FRACTION = 0.9f
