package io.github.prithvimk.gymtracker.ui.workout

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme

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
 * [field]: 2.5 kg, 1 rep or 0.5 RPE.
 */
@Composable
fun Keypad(field: SetField, actions: KeypadActions, modifier: Modifier = Modifier) {
    val step = formatSetNumber(field.stepSize)
    val stepDownLabel = stringResource(R.string.keypad_step_down, step)
    val stepUpLabel = stringResource(R.string.keypad_step_up, step)
    val stepDown = stringResource(R.string.keypad_step_down_description, step)
    val stepUp = stringResource(R.string.keypad_step_up_description, step)
    val delete = stringResource(R.string.keypad_delete)
    val hide = stringResource(R.string.keypad_hide)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        // Taps between keys stay here instead of reaching the screen, where they would close the keypad.
        modifier = modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures {} },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(KEY_GAP),
            modifier = Modifier.padding(KEY_GAP),
        ) {
            KeyRow {
                DigitKey('1', actions)
                DigitKey('2', actions)
                DigitKey('3', actions)
                Key(stepDownLabel, stepDown, KeyStyle.Accent) { actions.onStep(false) }
            }
            KeyRow {
                DigitKey('4', actions)
                DigitKey('5', actions)
                DigitKey('6', actions)
                Key(stepUpLabel, stepUp, KeyStyle.Accent) { actions.onStep(true) }
            }
            KeyRow {
                DigitKey('7', actions)
                DigitKey('8', actions)
                DigitKey('9', actions)
                Key("⌫", delete, KeyStyle.Plain) { actions.onKey(KeypadKey.Backspace) }
            }
            KeyRow {
                Key(".", null, KeyStyle.Plain, enabled = field != SetField.REPS) { actions.onKey(KeypadKey.Decimal) }
                DigitKey('0', actions)
                Key(
                    label = null,
                    description = hide,
                    style = KeyStyle.Plain,
                    icon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) },
                    onClick = actions.onHide,
                )
                Key(stringResource(R.string.keypad_next), null, KeyStyle.Primary, onClick = actions.onNext)
            }
        }
    }
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

/** One key: [label] as text unless [icon] is given. [description] is for keys whose label doesn't say what they do. */
@Composable
private fun RowScope.Key(
    label: String?,
    description: String?,
    style: KeyStyle,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
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
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = if (enabled) content else content.copy(alpha = DISABLED_ALPHA),
        modifier = Modifier.weight(1f).height(KEY_HEIGHT).then(described),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (icon != null) icon() else Text(label.orEmpty(), style = MaterialTheme.typography.titleLarge)
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
        Keypad(field = SetField.WEIGHT, actions = KeypadActions.None)
    }
}
