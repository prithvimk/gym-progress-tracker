package io.github.prithvimk.gymtracker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.timer.RestTimer
import io.github.prithvimk.gymtracker.timer.RestTimerState
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import kotlinx.coroutines.delay

/** Rest timer controls. [onAdjust] takes seconds to add, negative to take off. */
@Immutable
class RestActions(val onAdjust: (seconds: Int) -> Unit, val onSkip: () -> Unit) {
    companion object {
        val None = RestActions({}, {})
    }
}

/** The running rest: time left counting down, how far through it is, and controls to change or skip it. */
@Composable
fun RestBar(rest: RestTimerState, actions: RestActions, modifier: Modifier = Modifier) {
    val remaining by produceState(rest.remainingMillis(System.currentTimeMillis()), rest) {
        while (value > 0L) {
            // Tick on the second boundary of the time left, so the display never skips a second.
            delay(value % TICK_MS + 1L)
            value = rest.remainingMillis(System.currentTimeMillis())
        }
    }
    val progress = if (rest.durationMillis > 0L) remaining.toFloat() / rest.durationMillis else 0f
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        // Rounded up, so it reads 0:01 until it is really over.
                        text = formatElapsed(remaining + TICK_MS - 1L),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = rest.exerciseName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val subtract = stringResource(R.string.rest_subtract_description)
                    val add = stringResource(R.string.rest_add_description)
                    OutlinedButton(
                        onClick = { actions.onAdjust(-RestTimer.ADJUST_STEP_SECONDS) },
                        modifier = Modifier.semantics { contentDescription = subtract },
                    ) {
                        Text(stringResource(R.string.rest_subtract))
                    }
                    OutlinedButton(
                        onClick = { actions.onAdjust(RestTimer.ADJUST_STEP_SECONDS) },
                        modifier = Modifier.semantics { contentDescription = add },
                    ) {
                        Text(stringResource(R.string.rest_add))
                    }
                    TextButton(onClick = actions.onSkip) { Text(stringResource(R.string.rest_skip)) }
                }
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp, end = 8.dp),
            )
        }
    }
}

private const val TICK_MS = 1_000L

@Preview(showBackground = true)
@Composable
private fun RestBarPreview() {
    val rest = RestTimerState(System.currentTimeMillis() + 83_000L, 120_000L, "Barbell Bench Press")
    GymTrackerTheme(dynamicColor = false) {
        RestBar(rest = rest, actions = RestActions.None)
    }
}
