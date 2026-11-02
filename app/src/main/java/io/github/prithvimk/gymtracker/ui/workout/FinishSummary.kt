package io.github.prithvimk.gymtracker.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutRecord
import io.github.prithvimk.gymtracker.domain.ExerciseComparison
import io.github.prithvimk.gymtracker.domain.SessionComparison
import io.github.prithvimk.gymtracker.domain.TopSet
import io.github.prithvimk.gymtracker.domain.TopSetChange
import io.github.prithvimk.gymtracker.domain.WorkoutSummary
import io.github.prithvimk.gymtracker.domain.model.PrType
import io.github.prithvimk.gymtracker.ui.components.StatTile
import io.github.prithvimk.gymtracker.ui.format.durationText
import io.github.prithvimk.gymtracker.ui.format.percentChangeText
import io.github.prithvimk.gymtracker.ui.format.volumeText
import io.github.prithvimk.gymtracker.ui.format.weightText
import io.github.prithvimk.gymtracker.ui.history.formatDate
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import io.github.prithvimk.gymtracker.ui.theme.tabular
import java.time.Instant
import java.time.ZoneId

/**
 * The finish summary, in place of the Workout tab until it is dismissed: duration, volume and sets,
 * the records set, and how it compares with the last session of the same routine. [onResume] is null
 * when the workout can't be reopened. Back dismisses it, the same as Done.
 */
@Composable
internal fun FinishSummaryScreen(
    finished: FinishedWorkout,
    onDone: () -> Unit,
    onSaveAsRoutine: () -> Unit,
    onResume: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDone)
    val summary = finished.summary
    Column(modifier = modifier.fillMaxSize()) {
        // Resume undoes a Finish tapped by mistake, so it sits apart from Done, where a slip won't hit it.
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 8.dp),
        ) {
            if (onResume != null) {
                TextButton(onClick = onResume) { Text(stringResource(R.string.workout_summary_resume)) }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            Column {
                Text(
                    text = dayAndRoutineText(finished.startedAt, finished.routineName),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.workout_summary_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (finished.endedAutomatically) {
                Text(
                    text = stringResource(R.string.workout_summary_ended_automatically),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    durationText(summary.durationMillis),
                    stringResource(R.string.workout_summary_duration),
                    Modifier.weight(1f),
                )
                StatTile(
                    volumeText(summary.volumeKg),
                    stringResource(R.string.workout_summary_volume),
                    Modifier.weight(1.3f),
                )
                StatTile(
                    summary.setCount.toString(),
                    stringResource(R.string.workout_summary_sets),
                    Modifier.weight(0.7f),
                )
            }
            if (summary.setCount == 0) {
                Text(
                    text = stringResource(R.string.workout_summary_no_sets),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (finished.records.isNotEmpty()) RecordsCelebration(finished.records)
            finished.comparison?.let { ComparisonCard(it, finished.routineName) }
            Spacer(Modifier.height(8.dp))
        }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.workout_summary_done))
            }
            TextButton(onClick = onSaveAsRoutine, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.workout_save_as_routine))
            }
        }
    }
}

/** "Saturday 31 October · Push Day", or the day alone for a workout not started from a routine. */
@Composable
private fun dayAndRoutineText(startedAt: Long, routineName: String?): String {
    val day = formatDate(Instant.ofEpochMilli(startedAt).atZone(ZoneId.systemDefault()).toLocalDate(), "EEEEdMMMM")
    return if (routineName == null) day else stringResource(R.string.workout_summary_day_routine, day, routineName)
}

/** The volume change and each exercise's top set against the last session of the routine. */
@Composable
private fun ComparisonCard(comparison: SessionComparison, routineName: String?, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(
            text =
            if (routineName == null) {
                stringResource(R.string.workout_summary_compared_untitled)
            } else {
                stringResource(R.string.workout_summary_compared, routineName)
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        comparison.volumeChange?.let { change ->
            ComparisonRow(
                label = stringResource(R.string.workout_summary_volume),
                value = percentChangeText(change),
                was = stringResource(R.string.workout_summary_was, volumeText(comparison.previousVolumeKg)),
                change =
                when {
                    change > 0 -> TopSetChange.BETTER
                    change < 0 -> TopSetChange.WORSE
                    else -> TopSetChange.SAME
                },
            )
        }
        comparison.exercises.forEach { exercise ->
            val same = exercise.change == TopSetChange.SAME
            ComparisonRow(
                label = exercise.name,
                value = topSetText(exercise.topSet),
                was =
                if (same) {
                    stringResource(R.string.workout_summary_same)
                } else {
                    stringResource(R.string.workout_summary_was, topSetText(exercise.previousTopSet))
                },
                change = exercise.change,
            )
        }
    }
}

/**
 * One line of the comparison: [label] on the left, [value] on the right with an arrow for a clear
 * change, and what it [was] under it. The arrow is read out as "up" or "down", and the colour only
 * repeats it.
 */
@Composable
private fun ComparisonRow(label: String, value: String, was: String, change: TopSetChange) {
    val arrow =
        when (change) {
            TopSetChange.BETTER -> R.drawable.ic_trending_up to stringResource(R.string.workout_summary_change_up)
            TopSetChange.WORSE -> R.drawable.ic_trending_down to stringResource(R.string.workout_summary_change_down)
            TopSetChange.SAME, TopSetChange.MIXED -> null
        }
    val valueColor =
        if (change == TopSetChange.BETTER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val description = listOfNotNull(label, arrow?.second, value, was).joinToString(", ")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                arrow?.let { (icon, _) ->
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        // Quieter when it went down: a lighter day is no failure.
                        tint = if (change ==
                            TopSetChange.WORSE
                        ) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            valueColor
                        },
                        modifier = Modifier.size(18.dp).padding(end = 2.dp),
                    )
                }
                Text(value, style = MaterialTheme.typography.titleMedium.tabular(), color = valueColor)
            }
            Text(
                text = was,
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

/** "82.5 kg × 8", or "12 reps" for a bodyweight set. */
@Composable
private fun topSetText(top: TopSet): String = if (top.weightKg > 0) {
    stringResource(R.string.exercise_detail_set, weightText(top.weightKg), top.reps)
} else {
    pluralStringResource(R.plurals.exercise_detail_set_reps, top.reps, top.reps)
}

@Preview(showBackground = true)
@Composable
private fun FinishSummaryPreview() {
    val bench = WorkoutRecord(PrType.WEIGHT, 82.5, "s", "bench", "Bench Press", 82.5, 8)
    GymTrackerTheme(dynamicColor = false) {
        FinishSummaryScreen(
            finished =
            FinishedWorkout(
                workoutId = "preview",
                summary = WorkoutSummary(58 * 60_000L, 3, 17, 8_420.0),
                startedAt = System.currentTimeMillis(),
                routineName = "Push Day",
                records = listOf(ExerciseRecords("Bench Press", listOf(bench))),
                comparison =
                SessionComparison(
                    volumeKg = 8_420.0,
                    previousVolumeKg = 8_105.0,
                    exercises =
                    listOf(
                        ExerciseComparison("bench", "Bench Press", TopSet(82.5, 8), TopSet(80.0, 8)),
                        ExerciseComparison("ohp", "Overhead Press", TopSet(50.0, 8), TopSet(50.0, 8)),
                    ),
                ),
            ),
            onDone = {},
            onSaveAsRoutine = {},
            onResume = {},
        )
    }
}
