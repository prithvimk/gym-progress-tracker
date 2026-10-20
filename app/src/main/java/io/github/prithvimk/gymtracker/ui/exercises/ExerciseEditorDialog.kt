@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.prithvimk.gymtracker.ui.exercises

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.domain.model.Equipment
import io.github.prithvimk.gymtracker.domain.model.MuscleGroup

@Composable
fun ExerciseEditorDialog(
    state: ExerciseEditorState,
    onNameChange: (String) -> Unit,
    onMuscleGroupChange: (MuscleGroup) -> Unit,
    onEquipmentChange: (Equipment) -> Unit,
    onSave: () -> Unit,
    onToggleArchived: () -> Unit,
    onDismiss: () -> Unit,
) {
    val editable = state.isCustom && !state.isSaving
    val title =
        when {
            state.isNew -> stringResource(R.string.editor_title_new)
            state.isCustom -> stringResource(R.string.editor_title_edit)
            else -> state.name
        }
    val errorMessage = state.error?.let { errorText(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.isCustom) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = editable,
                        label = { Text(stringResource(R.string.editor_name)) },
                        supportingText = { if (errorMessage != null) Text(errorMessage) },
                        isError = errorMessage != null,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true,
                    )
                }
                SectionLabel(R.string.editor_muscle_group)
                ChipRow(
                    options = MuscleGroup.entries,
                    selected = state.muscleGroup,
                    enabled = editable,
                    onSelect = onMuscleGroupChange,
                    label = { it.labelRes },
                )
                SectionLabel(R.string.editor_equipment)
                ChipRow(
                    options = Equipment.entries,
                    selected = state.equipment,
                    enabled = editable,
                    onSelect = onEquipmentChange,
                    label = { it.labelRes },
                )
                if (!state.isCustom) {
                    Text(
                        text = stringResource(R.string.editor_builtin_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (state.isCustom) {
                TextButton(onClick = onSave, enabled = !state.isSaving) {
                    Text(stringResource(R.string.editor_save))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.editor_close))
                }
            }
        },
        dismissButton = {
            Row {
                if (!state.isNew) {
                    TextButton(onClick = onToggleArchived, enabled = !state.isSaving) {
                        Text(stringResource(if (state.archived) R.string.editor_restore else R.string.editor_archive))
                    }
                }
                if (state.isCustom) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.editor_cancel))
                    }
                }
            }
        },
    )
}

@Composable
private fun SectionLabel(@StringRes textRes: Int) {
    Text(text = stringResource(textRes), style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    enabled: Boolean,
    onSelect: (T) -> Unit,
    label: (T) -> Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(stringResource(label(option))) },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun errorText(error: EditorError): String = when (error) {
    EditorError.BlankName -> stringResource(R.string.editor_error_blank)
    is EditorError.DuplicateName -> stringResource(R.string.editor_error_duplicate, error.existingName)
    EditorError.NotEditable -> stringResource(R.string.editor_error_not_editable)
}
