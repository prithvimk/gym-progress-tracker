package io.github.prithvimk.gymtracker.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.matchPrevious
import io.github.prithvimk.gymtracker.domain.model.SetType

/** Set edits from the logger. Every callback takes the set's id. */
@Immutable
class SetActions(
    val onWeightChange: (setId: String, weightKg: Double) -> Unit,
    val onRepsChange: (setId: String, reps: Int) -> Unit,
    val onRpeChange: (setId: String, rpe: Double?) -> Unit,
    val onTypeChange: (setId: String, type: SetType) -> Unit,
    val onCompletedChange: (setId: String, completed: Boolean) -> Unit,
    val onDelete: (setId: String) -> Unit,
) {
    companion object {
        val None = SetActions({ _, _ -> }, { _, _ -> }, { _, _ -> }, { _, _ -> }, { _, _ -> }, {})
    }
}

/**
 * The sets of one exercise as a table, with a button to add another set. [previousSets] are the
 * exercise's sets from last session, shown beside their counterparts in the "Last" column.
 */
@Composable
fun SetTable(
    sets: List<WorkoutSet>,
    previousSets: List<WorkoutSet>,
    actions: SetActions,
    onAddSet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = setLabels(sets.map { it.type })
    val lastTime = matchPrevious(sets, previousSets)
    Column(modifier = modifier.fillMaxWidth()) {
        if (sets.isNotEmpty()) {
            SetRowLayout(
                labelCell = { HeaderText(stringResource(R.string.set_header_set)) },
                lastCell = { HeaderText(stringResource(R.string.set_header_last)) },
                weightCell = { HeaderText(stringResource(R.string.set_header_weight)) },
                repsCell = { HeaderText(stringResource(R.string.set_header_reps)) },
                rpeCell = { HeaderText(stringResource(R.string.set_header_rpe)) },
                doneCell = { HeaderText(stringResource(R.string.set_header_done)) },
            )
            sets.forEachIndexed { index, set -> SetRow(set, labels[index], lastTime[index], actions) }
        }
        TextButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.set_add), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun SetRow(set: WorkoutSet, label: SetLabel, lastTime: WorkoutSet?, actions: SetActions) {
    val completed = set.completedAt != null
    val labelText = label.text()
    val rowColor =
        if (completed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    SetRowLayout(
        modifier = Modifier.padding(vertical = 2.dp).background(rowColor, RoundedCornerShape(8.dp)),
        labelCell = { SetLabelButton(set, labelText, actions) },
        lastCell = { LastTimeText(lastTime) },
        weightCell = {
            SetNumberField(
                key = set.id,
                initial = formatSetNumber(set.weightKg),
                keyboardType = KeyboardType.Decimal,
                description = stringResource(R.string.set_weight_description, labelText),
                parse = { text -> parseWeight(text).ifValid { actions.onWeightChange(set.id, it) } },
            )
        },
        repsCell = {
            SetNumberField(
                key = set.id,
                initial = if (set.reps == 0) "" else set.reps.toString(),
                keyboardType = KeyboardType.Number,
                description = stringResource(R.string.set_reps_description, labelText),
                parse = { text -> parseReps(text).ifValid { actions.onRepsChange(set.id, it) } },
            )
        },
        rpeCell = {
            SetNumberField(
                key = set.id,
                initial = formatSetNumber(set.rpe),
                keyboardType = KeyboardType.Decimal,
                description = stringResource(R.string.set_rpe_description, labelText),
                parse = { text -> parseRpe(text).ifValid { actions.onRpeChange(set.id, it) } },
                imeAction = ImeAction.Done,
            )
        },
        doneCell = {
            val description = stringResource(R.string.set_complete_description, labelText)
            Checkbox(
                checked = completed,
                onCheckedChange = { actions.onCompletedChange(set.id, it) },
                // A set needs reps before it can count as done; a done set can always be unticked.
                enabled = completed || set.reps > 0,
                modifier = Modifier.semantics { contentDescription = description },
            )
        },
    )
}

@Composable
private fun SetLabelButton(set: WorkoutSet, labelText: String, actions: SetActions) {
    var menuOpen by remember { mutableStateOf(false) }
    val description = stringResource(R.string.set_options, labelText)
    Box {
        TextButton(
            onClick = { menuOpen = true },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.semantics { contentDescription = description },
        ) {
            Text(labelText, style = MaterialTheme.typography.titleMedium)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            SetType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.nameRes)) },
                    enabled = type != set.type,
                    onClick = {
                        menuOpen = false
                        actions.onTypeChange(set.id, type)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.set_delete)) },
                onClick = {
                    menuOpen = false
                    actions.onDelete(set.id)
                },
            )
        }
    }
}

/**
 * A number field that keeps what you type and saves it whenever [parse] accepts it, so a half-typed
 * "62." is never written back over. Text that does not parse is flagged and not saved.
 */
@Composable
private fun SetNumberField(
    key: String,
    initial: String,
    keyboardType: KeyboardType,
    description: String,
    parse: (String) -> Boolean,
    imeAction: ImeAction = ImeAction.Next,
) {
    var text by rememberSaveable(key) { mutableStateOf(initial) }
    var valid by remember(key) { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }
    // An outlined field with tighter padding than OutlinedTextField's 16dp, so five columns fit a phone.
    BasicTextField(
        value = text,
        onValueChange = {
            text = it
            valid = parse(it)
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FIELD_MIN_HEIGHT)
            .semantics { contentDescription = description },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        singleLine = true,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = text,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                isError = !valid,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
            )
        },
    )
}

@Composable
private fun SetRowLayout(
    labelCell: @Composable () -> Unit,
    lastCell: @Composable () -> Unit,
    weightCell: @Composable () -> Unit,
    repsCell: @Composable () -> Unit,
    rpeCell: @Composable () -> Unit,
    doneCell: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(LABEL_WIDTH), contentAlignment = Alignment.Center) { labelCell() }
        Box(Modifier.weight(LAST_COLUMN), contentAlignment = Alignment.Center) { lastCell() }
        Box(Modifier.weight(WEIGHT_COLUMN), contentAlignment = Alignment.Center) { weightCell() }
        Box(Modifier.weight(REPS_COLUMN), contentAlignment = Alignment.Center) { repsCell() }
        Box(Modifier.weight(RPE_COLUMN), contentAlignment = Alignment.Center) { rpeCell() }
        Box(Modifier.width(DONE_WIDTH), contentAlignment = Alignment.Center) { doneCell() }
    }
}

/** Last session's counterpart of a set as "80 × 8", or a dash when there is none. */
@Composable
private fun LastTimeText(lastTime: WorkoutSet?) {
    val text =
        lastTime?.let { stringResource(R.string.set_last_value, formatSetNumber(it.weightKg).ifEmpty { "0" }, it.reps) }
            ?: stringResource(R.string.set_last_none)
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 2,
    )
}

@Composable
private fun HeaderText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SetLabel.text(): String = when (this) {
    is SetLabel.Number -> value.toString()
    is SetLabel.Kind -> stringResource(type.shortRes)
}

private val SetType.nameRes: Int
    get() =
        when (this) {
            SetType.WARMUP -> R.string.set_type_warmup
            SetType.WORKING -> R.string.set_type_working
            SetType.DROP -> R.string.set_type_drop
            SetType.FAILURE -> R.string.set_type_failure
        }

private val SetType.shortRes: Int
    get() =
        when (this) {
            SetType.WARMUP -> R.string.set_short_warmup
            SetType.WORKING -> R.string.set_short_working
            SetType.DROP -> R.string.set_short_drop
            SetType.FAILURE -> R.string.set_short_failure
        }

/** Runs [save] with the value if the input is valid, and says whether it was. */
private inline fun <T> FieldInput<T>.ifValid(save: (T) -> Unit): Boolean = when (this) {
    is FieldInput.Valid -> {
        save(value)
        true
    }
    FieldInput.Invalid -> false
}

private val FIELD_MIN_HEIGHT = 48.dp
private val LABEL_WIDTH = 44.dp
private val DONE_WIDTH = 44.dp
private const val LAST_COLUMN = 1.1f
private const val WEIGHT_COLUMN = 1.2f
private const val REPS_COLUMN = 1f
private const val RPE_COLUMN = 0.9f
