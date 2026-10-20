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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.db.entity.Exercise
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme

@Composable
fun ExercisesRoute(
    modifier: Modifier = Modifier,
    viewModel: ExercisesViewModel = viewModel(factory = ExercisesViewModel.factory),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val list by viewModel.list.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()

    ExercisesScreen(
        filter = filter,
        list = list,
        onQueryChange = viewModel::onQueryChange,
        onMuscleGroupSelect = viewModel::onMuscleGroupSelect,
        onShowArchivedChange = viewModel::onShowArchivedChange,
        onExerciseClick = viewModel::onExerciseClick,
        onCreateClick = viewModel::onCreateClick,
        modifier = modifier,
    )

    editor?.let { state ->
        ExerciseEditorDialog(
            state = state,
            onNameChange = viewModel::onEditorNameChange,
            onMuscleGroupChange = viewModel::onEditorMuscleGroupChange,
            onEquipmentChange = viewModel::onEditorEquipmentChange,
            onSave = viewModel::onEditorSave,
            onToggleArchived = viewModel::onEditorToggleArchived,
            onDismiss = viewModel::onEditorDismiss,
        )
    }
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
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.exercises_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )
            SearchField(query = filter.query, onQueryChange = onQueryChange)
            MuscleGroupChips(selected = filter.muscleGroup, onSelect = onMuscleGroupSelect)
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

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.exercises_search)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.exercises_clear_search))
                }
            }
        },
        singleLine = true,
    )
}

@Composable
private fun MuscleGroupChips(selected: MuscleGroup?, onSelect: (MuscleGroup?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.exercises_filter_all)) },
            )
        }
        items(MuscleGroup.entries) { group ->
            FilterChip(
                selected = selected == group,
                onClick = { onSelect(if (selected == group) null else group) },
                label = { Text(stringResource(group.labelRes)) },
            )
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
                    ExerciseRow(exercise = exercise, onClick = { onClick(exercise) })
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
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
        supportingContent = { Text(stringResource(R.string.exercise_subtitle, muscle, equipment)) },
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
): Exercise {
    return Exercise(
        id = id,
        name = name,
        muscleGroup = muscleGroup,
        equipment = equipment,
        isCustom = isCustom,
        archived = false,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
