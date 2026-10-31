@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.prithvimk.gymtracker.ui.workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet
import io.github.prithvimk.gymtracker.domain.matchPrevious
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.domain.model.SetType
import io.github.prithvimk.gymtracker.ui.components.RecordDisc
import io.github.prithvimk.gymtracker.ui.format.weightNumberText
import io.github.prithvimk.gymtracker.ui.theme.recordColors
import io.github.prithvimk.gymtracker.ui.theme.tabular

/** Set edits from the logger. Every callback takes the set's id. */
@Immutable
class SetActions(
    val onTypeChange: (setId: String, type: SetType) -> Unit,
    val onCompletedChange: (setId: String, completed: Boolean) -> Unit,
    val onDelete: (setId: String) -> Unit,
    val onFieldClick: (setId: String, field: SetField) -> Unit,
) {
    companion object {
        val None = SetActions({ _, _ -> }, { _, _ -> }, {}, { _, _ -> })
    }
}

/**
 * The sets of one exercise as a table, with a button to add another set. [previousSets] are the
 * exercise's sets from last session, shown beside their counterparts in the "Last" column. [badges]
 * maps each set id that holds a record to its record types, shown as a PR badge on the set. [keypad]
 * is the keypad's edit, if it is typing into one of these sets.
 */
@Composable
fun SetTable(
    sets: List<WorkoutSet>,
    previousSets: List<WorkoutSet>,
    keypad: KeypadState?,
    actions: SetActions,
    onAddSet: () -> Unit,
    modifier: Modifier = Modifier,
    badges: Map<String, List<PrType>> = emptyMap(),
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
            sets.forEachIndexed { index, set ->
                SetRow(
                    set,
                    labels[index],
                    lastTime[index],
                    badges[set.id].orEmpty(),
                    keypad?.takeIf { it.target.setId == set.id },
                    actions,
                )
            }
        }
        TextButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.set_add), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun SetRow(
    set: WorkoutSet,
    label: SetLabel,
    lastTime: WorkoutSet?,
    badge: List<PrType>,
    keypad: KeypadState?,
    actions: SetActions,
) {
    val completed = set.completedAt != null
    val labelText = label.text()
    val rowColor = if (completed) MaterialTheme.colorScheme.primary.copy(alpha = DONE_ROW_ALPHA) else Color.Transparent
    SetRowLayout(
        modifier = Modifier.padding(vertical = 2.dp).background(rowColor, RoundedCornerShape(8.dp)),
        labelCell = { SetLabelButton(set, labelText, badge, actions) },
        lastCell = { LastTimeText(lastTime) },
        weightCell = {
            SetValueField(
                set,
                SetField.WEIGHT,
                keypad,
                stringResource(R.string.set_weight_description, labelText),
                actions
            )
        },
        repsCell = {
            SetValueField(set, SetField.REPS, keypad, stringResource(R.string.set_reps_description, labelText), actions)
        },
        rpeCell = {
            SetValueField(set, SetField.RPE, keypad, stringResource(R.string.set_rpe_description, labelText), actions)
        },
        doneCell = {
            DoneCheck(
                checked = completed,
                onCheckedChange = { actions.onCompletedChange(set.id, it) },
                // A set needs reps before it can count as done; a done set can always be unticked.
                enabled = completed || set.reps > 0,
                description = stringResource(R.string.set_complete_description, labelText),
            )
        },
    )
}

/**
 * The set's label, which opens its menu. A set holding records shows its label on a gold disc, read
 * out as "Set 1, personal record: heaviest weight"; the disc pops in when the set ticked off sets one.
 */
@Composable
private fun SetLabelButton(set: WorkoutSet, labelText: String, records: List<PrType>, actions: SetActions) {
    var menuOpen by remember { mutableStateOf(false) }
    val description =
        if (records.isEmpty()) {
            stringResource(R.string.set_label_description, labelText)
        } else {
            stringResource(
                R.string.set_label_record_description,
                labelText,
                records.map { stringResource(it.badgeRes) }.joinToString(),
            )
        }
    val optionsLabel = stringResource(R.string.set_options_action)
    Box {
        TextButton(
            onClick = { menuOpen = true },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(label = optionsLabel) {
                    menuOpen = true
                    true
                }
            },
        ) {
            val hasRecord = records.isNotEmpty()
            // A record set's label sits on the disc; others keep plain text in the same place.
            AnimatedContent(
                targetState = hasRecord,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                        .togetherWith(scaleOut() + fadeOut())
                },
                label = "set label",
            ) { record ->
                if (record) {
                    RecordDisc(labelText)
                } else {
                    Text(labelText, style = MaterialTheme.typography.titleMedium.tabular())
                }
            }
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

/** The PR badge for a set holding records of [types], read out as the records it holds. */
@Composable
internal fun RecordBadge(types: List<PrType>, modifier: Modifier = Modifier) {
    val description =
        stringResource(R.string.set_record_description, types.map { stringResource(it.badgeRes) }.joinToString())
    Badge(
        containerColor = MaterialTheme.recordColors.record,
        contentColor = MaterialTheme.recordColors.onRecord,
        modifier = modifier.semantics { contentDescription = description },
    ) {
        Text(stringResource(R.string.set_record_badge))
    }
}

/**
 * A set's number, edited with the keypad rather than the system keyboard. While the keypad is on it
 * the field shows the keypad's text, outlined, with the whole value highlighted until typing starts
 * over it; text that isn't a value yet is flagged.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetValueField(
    set: WorkoutSet,
    field: SetField,
    keypad: KeypadState?,
    description: String,
    actions: SetActions,
) {
    val editing = keypad?.takeIf { it.field == field }
    val text = editing?.text ?: set.fieldText(field)
    val colors = MaterialTheme.colorScheme
    val borderColor =
        when {
            editing != null && !editing.isValid -> colors.error
            editing != null -> colors.primary
            else -> Color.Transparent
        }
    val focusManager = LocalFocusManager.current
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(editing != null) {
        // Wait a frame so the list has shrunk for the keypad before scrolling this row clear of it.
        if (editing != null) {
            withFrameNanos {}
            bringIntoView.bringIntoView()
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FIELD_MIN_HEIGHT)
            .bringIntoViewRequester(bringIntoView)
            .clip(FIELD_SHAPE)
            .background(colors.surfaceContainerLowest)
            .border(2.dp, borderColor, FIELD_SHAPE)
            .clickable {
                // A note may have the system keyboard up; the keypad takes over from it.
                focusManager.clearFocus()
                actions.onFieldClick(set.id, field)
            }.semantics { contentDescription = description },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.tabular(),
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .then(
                    if (editing?.replaceOnType == true && text.isNotEmpty()) {
                        Modifier.background(colors.primary.copy(alpha = SELECTION_ALPHA), RoundedCornerShape(2.dp))
                    } else {
                        Modifier
                    },
                ).padding(horizontal = 2.dp),
        )
    }
}

/**
 * The done tick: a filled primary square with a check when done, an empty outlined one otherwise,
 * inside a 48 dp touch target. Read out as a checkbox.
 */
@Composable
private fun DoneCheck(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean, description: String) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(DONE_TARGET)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .semantics { contentDescription = description },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DONE_BOX)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .then(
                    if (checked) {
                        Modifier.background(colors.primary, shape)
                    } else {
                        Modifier.background(colors.surfaceContainerLowest, shape).border(2.dp, colors.outline, shape)
                    },
                ),
        ) {
            if (checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
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
        lastTime?.let { stringResource(R.string.set_last_value, weightNumberText(it.weightKg), it.reps) }
            ?: stringResource(R.string.set_last_none)
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.tabular(),
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
internal fun SetLabel.text(): String = when (this) {
    is SetLabel.Number -> value.toString()
    is SetLabel.Kind -> stringResource(type.shortRes)
}

private val PrType.badgeRes: Int
    get() =
        when (this) {
            PrType.WEIGHT -> R.string.set_record_weight
            PrType.REPS_AT_WEIGHT -> R.string.set_record_reps_at_weight
            PrType.E1RM -> R.string.set_record_e1rm
            PrType.VOLUME -> R.string.set_record_volume
        }

internal val SetType.nameRes: Int
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

private val FIELD_MIN_HEIGHT = 48.dp
private val DONE_TARGET = 48.dp
private val DONE_BOX = 32.dp
private val FIELD_SHAPE = RoundedCornerShape(8.dp)
private const val SELECTION_ALPHA = 0.3f
private const val DONE_ROW_ALPHA = 0.12f
private const val DISABLED_ALPHA = 0.38f
private val LABEL_WIDTH = 44.dp
private val DONE_WIDTH = 48.dp
private const val LAST_COLUMN = 1.1f
private const val WEIGHT_COLUMN = 1.2f
private const val REPS_COLUMN = 1f
private const val RPE_COLUMN = 0.9f
