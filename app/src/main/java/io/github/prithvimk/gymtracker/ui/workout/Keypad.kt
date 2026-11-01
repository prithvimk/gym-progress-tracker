package io.github.prithvimk.gymtracker.ui.workout

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.ui.format.weightText
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import io.github.prithvimk.gymtracker.ui.theme.tabular

/** What the keypad does. */
@Immutable
class KeypadActions(
    val onKey: (KeypadKey) -> Unit,
    val onStep: (up: Boolean) -> Unit,
    val onNext: () -> Unit,
    val onHide: () -> Unit,
) {
    companion object {
        val None = KeypadActions({}, {}, {}, {})
    }
}

/**
 * A number pad for set fields that stays up while moving between them, with steppers sized to
 * [field]: 2.5 kg, 1 rep or 0.5 RPE. Along its top, [context] says what it is typing into, beside the
 * button that hides it.
 */
@Composable
fun Keypad(field: SetField, actions: KeypadActions, modifier: Modifier = Modifier, context: KeypadContext? = null) {
    val step = stepText(field)
    val stepDown = stringResource(R.string.keypad_step_down_description, step)
    val stepUp = stringResource(R.string.keypad_step_up_description, step)
    val delete = stringResource(R.string.keypad_delete)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        // Taps between keys stay here instead of reaching the screen, where they would close the keypad.
        modifier = modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures {} },
    ) {
        Column {
            ContextLine(context, actions.onHide)
            Column(
                verticalArrangement = Arrangement.spacedBy(KEY_GAP),
                modifier = Modifier.padding(start = KEY_GAP, end = KEY_GAP, bottom = KEY_GAP),
            ) {
                KeyRow {
                    DigitKey('1', actions)
                    DigitKey('2', actions)
                    DigitKey('3', actions)
                    Key(stringResource(R.string.keypad_step_down, step), stepDown, KeyStyle.Accent) {
                        actions.onStep(false)
                    }
                }
                KeyRow {
                    DigitKey('4', actions)
                    DigitKey('5', actions)
                    DigitKey('6', actions)
                    Key(stringResource(R.string.keypad_step_up, step), stepUp, KeyStyle.Accent) { actions.onStep(true) }
                }
                KeyRow {
                    DigitKey('7', actions)
                    DigitKey('8', actions)
                    DigitKey('9', actions)
                    Key("⌫", delete, KeyStyle.Plain) { actions.onKey(KeypadKey.Backspace) }
                }
                KeyRow {
                    // Nested so Next spans two keys and the gap between them exactly.
                    Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP), modifier = Modifier.weight(1f)) {
                        // Reps are whole, so the decimal point is greyed out for them.
                        Key(".", null, KeyStyle.Plain, enabled = field != SetField.REPS) {
                            actions.onKey(KeypadKey.Decimal)
                        }
                        DigitKey('0', actions)
                    }
                    Key(stringResource(R.string.keypad_next), null, KeyStyle.Primary, onClick = actions.onNext)
                }
            }
        }
    }
}

/** "Bench Press · Set 3 · Reps", last time's set on the right, and the hide button. */
@Composable
private fun ContextLine(context: KeypadContext?, onHide: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
    ) {
        if (context != null) {
            val setName =
                when (val label = context.label) {
                    is SetLabel.Number -> stringResource(R.string.keypad_set_number, label.value)
                    is SetLabel.Kind -> stringResource(label.type.nameRes)
                }
            Text(
                text = stringResource(
                    R.string.keypad_context,
                    context.exerciseName,
                    setName,
                    stringResource(context.field.nameRes),
                ),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            context.lastTime?.let {
                Text(
                    text = stringResource(
                        R.string.keypad_last,
                        stringResource(R.string.set_last_value, weightText(it.weightKg), it.reps),
                    ),
                    style = MaterialTheme.typography.labelLarge.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        IconButton(onClick = onHide) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.keypad_hide))
        }
    }
}

/** One step of [field] with its unit: "2.5 kg", "1 rep" or "0.5" for RPE. */
@Composable
private fun stepText(field: SetField): String = when (field) {
    SetField.WEIGHT -> weightText(field.stepSize)
    SetField.REPS -> pluralStringResource(R.plurals.keypad_step_reps, 1, 1)
    SetField.RPE -> formatSetNumber(field.stepSize)
}

private val SetField.nameRes: Int
    get() =
        when (this) {
            SetField.WEIGHT -> R.string.keypad_field_weight
            SetField.REPS -> R.string.keypad_field_reps
            SetField.RPE -> R.string.keypad_field_rpe
        }

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(KEY_GAP), modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun RowScope.DigitKey(digit: Char, actions: KeypadActions) {
    Key(digit.toString(), null, KeyStyle.Plain) { actions.onKey(KeypadKey.Digit(digit)) }
}

private enum class KeyStyle { Plain, Accent, Primary }

/**
 * One key showing [label]. [description] is for keys whose label doesn't say what they do.
 */
@Composable
private fun RowScope.Key(
    label: String,
    description: String?,
    style: KeyStyle,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val described = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
    val colors = MaterialTheme.colorScheme
    val (container, content) =
        when (style) {
            KeyStyle.Plain -> colors.surfaceContainerHighest to colors.onSurface
            KeyStyle.Accent -> colors.secondaryContainer to colors.onSecondaryContainer
            KeyStyle.Primary -> colors.primary to colors.onPrimary
        }
    // Step keys carry a unit, so they use a smaller size to fit it.
    val textStyle = if (style ==
        KeyStyle.Accent
    ) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.titleLarge
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = if (enabled) content else content.copy(alpha = DISABLED_ALPHA),
        modifier = Modifier.weight(1f).height(KEY_HEIGHT).then(described),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(label, style = textStyle.tabular(), maxLines = 1)
        }
    }
}

private val KEY_HEIGHT = 52.dp
private val KEY_GAP = 6.dp
private const val DISABLED_ALPHA = 0.38f

@Preview(showBackground = true)
@Composable
private fun KeypadPreview() {
    GymTrackerTheme(dynamicColor = false) {
        Keypad(
            field = SetField.WEIGHT,
            actions = KeypadActions.None,
            context = KeypadContext("Barbell Bench Press", SetLabel.Number(3), SetField.WEIGHT, lastTime = null),
        )
    }
}
