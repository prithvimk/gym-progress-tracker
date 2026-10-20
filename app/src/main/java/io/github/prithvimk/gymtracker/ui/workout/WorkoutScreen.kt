package io.github.prithvimk.gymtracker.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.db.entity.Workout
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import kotlinx.coroutines.delay

@Composable
fun WorkoutRoute(
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val confirmingDiscard by viewModel.confirmingDiscard.collectAsStateWithLifecycle()

    WorkoutScreen(
        state = state,
        onStartClick = viewModel::onStartClick,
        onFinishClick = viewModel::onFinishClick,
        onDiscardClick = viewModel::onDiscardClick,
        modifier = modifier,
    )

    if (confirmingDiscard) {
        AlertDialog(
            onDismissRequest = viewModel::onDiscardDismiss,
            title = { Text(stringResource(R.string.workout_discard_title)) },
            text = { Text(stringResource(R.string.workout_discard_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDiscardConfirm) {
                    Text(stringResource(R.string.workout_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDiscardDismiss) {
                    Text(stringResource(R.string.workout_discard_keep))
                }
            },
        )
    }
}

@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    onStartClick: () -> Unit,
    onFinishClick: () -> Unit,
    onDiscardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        WorkoutUiState.Loading ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        WorkoutUiState.Idle -> IdleContent(onStartClick, modifier)

        is WorkoutUiState.Active ->
            ActiveContent(state.workout, onFinishClick, onDiscardClick, modifier)
    }
}

@Composable
private fun IdleContent(onStartClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.workout_idle_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.workout_idle_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStartClick) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text(stringResource(R.string.workout_start_empty), modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun ActiveContent(
    workout: Workout,
    onFinishClick: () -> Unit,
    onDiscardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Elapsed time is derived from startedAt, so it stays right across process death.
    val elapsed by produceState(System.currentTimeMillis() - workout.startedAt, workout.startedAt) {
        while (true) {
            value = System.currentTimeMillis() - workout.startedAt
            delay(TICK_MS)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.workout_active_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = formatElapsed(elapsed),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.workout_no_exercises),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onDiscardClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.workout_discard))
            }
            Button(onClick = onFinishClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.workout_finish))
            }
        }
    }
}

private const val TICK_MS = 1_000L

@Preview(showBackground = true)
@Composable
private fun IdlePreview() {
    GymTrackerTheme(dynamicColor = false) {
        WorkoutScreen(WorkoutUiState.Idle, onStartClick = {}, onFinishClick = {}, onDiscardClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ActivePreview() {
    val workout =
        Workout(
            id = "preview",
            routineId = null,
            startedAt = System.currentTimeMillis() - 754_000L,
            endedAt = null,
            notes = "",
            createdAt = 0L,
            updatedAt = 0L,
        )
    GymTrackerTheme(dynamicColor = false) {
        WorkoutScreen(WorkoutUiState.Active(workout), onStartClick = {}, onFinishClick = {}, onDiscardClick = {})
    }
}
