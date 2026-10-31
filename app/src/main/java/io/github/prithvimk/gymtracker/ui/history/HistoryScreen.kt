package io.github.prithvimk.gymtracker.ui.history

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutHistoryRow
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import io.github.prithvimk.gymtracker.ui.workout.formatElapsed
import io.github.prithvimk.gymtracker.ui.workout.formatVolume
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryRoute(
    onWorkoutClick: (workoutId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(state, onWorkoutClick, modifier)
}

@Composable
fun HistoryScreen(state: HistoryUiState, onWorkoutClick: (workoutId: String) -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        HistoryUiState.Loading ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        is HistoryUiState.Loaded ->
            if (state.weeks.isEmpty()) EmptyHistory(modifier) else HistoryList(state.weeks, onWorkoutClick, modifier)
    }
}

@Composable
private fun HistoryList(
    weeks: List<HistoryWeek>,
    onWorkoutClick: (workoutId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.nav_history),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        weeks.forEach { week ->
            item(key = "week-${week.weekStart}") { WeekHeader(week) }
            items(week.workouts, key = { it.id }) { workout -> WorkoutCard(workout, { onWorkoutClick(workout.id) }) }
        }
    }
}

/** "This week", then how many workouts and how much volume, e.g. "3 workouts · 12,340 kg". */
@Composable
private fun WeekHeader(week: HistoryWeek) {
    val count = week.workouts.size
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
    ) {
        Text(
            text = weekLabelText(week.label),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text =
            stringResource(
                R.string.history_week_totals,
                pluralStringResource(R.plurals.history_week_workouts, count, count),
                formatVolume(week.volumeKg),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun WorkoutCard(workout: WorkoutHistoryRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = workout.routineName ?: stringResource(R.string.history_workout_untitled),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = formatStartTime(workout.startedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                stringResource(
                    R.string.history_workout_stats,
                    formatElapsed(workout.endedAt - workout.startedAt),
                    pluralStringResource(R.plurals.history_workout_sets, workout.setCount, workout.setCount),
                    stringResource(R.string.workout_summary_volume_value, formatVolume(workout.volumeKg)),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (workout.recordCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trophy),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.history_workout_records,
                            workout.recordCount,
                            workout.recordCount
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.nav_history), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(R.string.history_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun weekLabelText(label: WeekLabel): String = when (label) {
    WeekLabel.ThisWeek -> stringResource(R.string.history_this_week)
    WeekLabel.LastWeek -> stringResource(R.string.history_last_week)
    is WeekLabel.StartingOn ->
        stringResource(R.string.history_week_of, formatDate(label.date, if (label.sameYear) "dMMM" else "dMMMy"))
}

/** A date in the user's locale, from a skeleton such as "dMMM" ("13 Oct" or "Oct 13"). */
@Composable
internal fun formatDate(date: LocalDate, skeleton: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(date)
}

/** When a workout started, e.g. "Tue 21 Oct, 18:05", in 12- or 24-hour time as the phone is set. */
@Composable
internal fun formatStartTime(startedAt: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    val skeleton = if (DateFormat.is24HourFormat(LocalContext.current)) "EEEdMMMHm" else "EEEdMMMhm"
    val formatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    return formatter.format(Instant.ofEpochMilli(startedAt).atZone(ZoneId.systemDefault()))
}

@Preview(showBackground = true)
@Composable
private fun HistoryPreview() {
    val now = System.currentTimeMillis()
    val hour = 3_600_000L
    val workouts =
        listOf(
            WorkoutHistoryRow(
                "a",
                "Push",
                now - 2 * hour,
                now - hour,
                setCount = 18,
                volumeKg = 8_420.0,
                recordCount = 3
            ),
            WorkoutHistoryRow(
                "b",
                null,
                now - 50 * hour,
                now - 49 * hour,
                setCount = 12,
                volumeKg = 5_100.5,
                recordCount = 0
            ),
        )
    GymTrackerTheme(dynamicColor = false) {
        HistoryScreen(
            HistoryUiState.Loaded(listOf(HistoryWeek(WeekLabel.ThisWeek, LocalDate.now(), workouts))),
            onWorkoutClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyHistoryPreview() {
    GymTrackerTheme(dynamicColor = false) { HistoryScreen(HistoryUiState.Loaded(emptyList()), onWorkoutClick = {}) }
}
