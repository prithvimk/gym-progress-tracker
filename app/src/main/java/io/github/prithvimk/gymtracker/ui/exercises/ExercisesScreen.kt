@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.prithvimk.gymtracker.ui.exercises

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.domain.LastTopSet
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.ui.format.relativeDayText
import io.github.prithvimk.gymtracker.ui.format.topSetText
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import io.github.prithvimk.gymtracker.ui.theme.tabular

@Composable
fun ExercisesRoute(
    onExerciseClick: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.factory),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()

    ExercisesScreen(
        filter = filter,
        list = list,
        onQueryChange = viewModel::onQueryChange,
        onMuscleGroupSelect = viewModel::onMuscleGroupSelect,
        onEquipmentSelect = viewModel::onEquipmentSelect,
        onShowArchivedChange = viewModel::onShowArchivedChange,
        onExerciseClick = onExerciseClick,
        onCreateClick = viewModel::onCreateClick,
        modifier = modifier,
    )

    ExerciseEditorHost(viewModel.editor)
}

@Composable
fun ExercisesScreen(
    filter: ExerciseFilter,
    list: ExerciseListState,
    onQueryChange: (String) -> Unit,
    onMuscleGroupSelect: (MuscleGroup?) -> Unit,
    onShowArchivedChange: (Boolean) -> Unit,
    onExerciseClick: (Exercise) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEquipmentSelect: (Equipment?) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.exercises_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )
            ExerciseSearchBar(query = filter.query, count = list.libraryCount, onQueryChange = onQueryChange)
            FilterChips(
                filter = filter,
                onMuscleGroupSelect = onMuscleGroupSelect,
                onEquipmentSelect = onEquipmentSelect
            )
            if (list.hasArchived || filter.showArchived) {
                ShowArchivedRow(checked = filter.showArchived, onCheckedChange = onShowArchivedChange)
            }
            ExerciseList(list = list, onClick = onExerciseClick, modifier = Modifier.weight(1f))
        }
        ExtendedFloatingActionButton(
            onClick = onCreateClick,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.exercises_new)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

/** A Material 3 search bar, always collapsed: the list under it is the results. */
@Composable
private fun ExerciseSearchBar(query: String, count: Int, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    Surface(
        shape = SearchBarDefaults.inputFieldShape,
        color = SearchBarDefaults.colors().containerColor,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SearchBarDefaults.InputField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { focusManager.clearFocus() },
            expanded = false,
            onExpandedChange = {},
            placeholder = {
                Text(
                    if (count > 0) {
                        pluralStringResource(R.plurals.exercises_search_count, count, count)
                    } else {
                        stringResource(R.string.exercises_search)
                    },
                )
            },
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

/**
 * The Equipment chip, which opens a menu, then All and a chip per muscle group. Equipment comes first
 * so it is on screen without scrolling the row.
 */
@Composable
private fun FilterChips(
    filter: ExerciseFilter,
    onMuscleGroupSelect: (MuscleGroup?) -> Unit,
    onEquipmentSelect: (Equipment?) -> Unit,
) {
    val selected = filter.muscleGroup
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { EquipmentChip(filter.equipment, onEquipmentSelect) }
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onMuscleGroupSelect(null) },
                label = { Text(stringResource(R.string.exercises_filter_all)) },
            )
        }
        items(MuscleGroup.entries) { group ->
            FilterChip(
                selected = selected == group,
                onClick = { onMuscleGroupSelect(if (selected == group) null else group) },
                label = { Text(stringResource(group.labelRes)) },
            )
        }
    }
}

/** "Equipment ▾", or the equipment picked, with a menu of every kind and Any equipment. */
@Composable
private fun EquipmentChip(equipment: Equipment?, onSelect: (Equipment?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = equipment != null,
            onClick = { open = true },
            label = { Text(stringResource(equipment?.labelRes ?: R.string.exercises_equipment)) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.exercises_any_equipment)) },
                onClick = {
                    open = false
                    onSelect(null)
                },
            )
            Equipment.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun ShowArchivedRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.exercises_show_archived),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ExerciseList(list: ExerciseListState, onClick: (Exercise) -> Unit, modifier: Modifier = Modifier) {
    when {
        list.isLoading -> {
            Box(modifier = modifier.fillMaxWidth())
        }
        list.exercises.isEmpty() -> {
            Text(
                text = stringResource(R.string.exercises_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.fillMaxWidth().padding(24.dp),
            )
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxWidth(),
                // Leaves room for the floating button over the last row.
                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            ) {
                items(list.exercises, key = { it.id }) { exercise ->
                    ExerciseRow(exercise = exercise, last = list.lastTopSets[exercise.id], onClick = {
                        onClick(exercise)
                    })
                }
            }
        }
    }
}

/** An exercise with its muscle group and equipment, and for one you've trained, its [last] top set and when. */
@Composable
private fun ExerciseRow(exercise: Exercise, last: LastTopSet?, onClick: () -> Unit) {
    val muscle = stringResource(exercise.muscleGroup.labelRes)
    val equipment = stringResource(exercise.equipment.labelRes)
    val tagRes =
        when {
            exercise.archived -> R.string.exercises_archived_tag
            exercise.isCustom -> R.string.exercises_custom_tag
            else -> null
        }
    ListItem(
        headlineContent = { Text(exercise.name) },
        supportingContent = {
            Column {
                Text(stringResource(R.string.exercise_subtitle, muscle, equipment))
                if (last != null) {
                    Text(
                        text =
                        stringResource(
                            R.string.exercises_last_top_set,
                            topSetText(last.topSet),
                            relativeDayText(last.startedAt),
                        ),
                        style = MaterialTheme.typography.bodyMedium.tabular(),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        trailingContent = {
            if (tagRes != null) {
                Text(
                    text = stringResource(tagRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun ExercisesScreenPreview() {
    val exercises =
        listOf(
            previewExercise("1", "Barbell Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, isCustom = false),
            previewExercise("2", "Lat Pulldown", MuscleGroup.BACK, Equipment.CABLE, isCustom = false),
            previewExercise("3", "Landmine Press", MuscleGroup.SHOULDERS, Equipment.BARBELL, isCustom = true),
        )
    GymTrackerTheme(dynamicColor = false) {
        ExercisesScreen(
            filter = ExerciseFilter(),
            list = ExerciseListState(exercises = exercises, isLoading = false),
            onQueryChange = {},
            onMuscleGroupSelect = {},
            onShowArchivedChange = {},
            onExerciseClick = {},
            onCreateClick = {},
        )
    }
}

private fun previewExercise(
    id: String,
    name: String,
    muscleGroup: MuscleGroup,
    equipment: Equipment,
    isCustom: Boolean,
): Exercise = Exercise(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    equipment = equipment,
    isCustom = isCustom,
    archived = false,
    createdAt = 0L,
    updatedAt = 0L,
)
