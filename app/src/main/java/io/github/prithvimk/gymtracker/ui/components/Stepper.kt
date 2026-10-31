package io.github.prithvimk.gymtracker.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.ui.format.formatClock

/** A label with its value between − and + buttons. */
@Composable
fun Stepper(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        val decreaseDescription = stringResource(R.string.routine_decrease, label)
        IconButton(
            onClick = onDecrease,
            enabled = canDecrease,
            modifier = Modifier.semantics { contentDescription = decreaseDescription },
        ) {
            // Material's core icons have no minus, and a typographic one matches the plus well enough.
            Text(text = "−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = VALUE_MIN_WIDTH),
        )
        IconButton(onClick = onIncrease, enabled = canIncrease) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.routine_increase, label))
        }
    }
}

/** A rest time as "2:00", or "Off" for none. */
@Composable
fun restText(seconds: Int): String =
    if (seconds <= 0) stringResource(R.string.rest_off) else formatClock(seconds * MILLIS_PER_SECOND)

private val VALUE_MIN_WIDTH = 56.dp
private const val MILLIS_PER_SECOND = 1_000L
