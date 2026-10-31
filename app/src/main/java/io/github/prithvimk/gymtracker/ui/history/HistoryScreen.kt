package io.github.prithvimk.gymtracker.ui.history

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
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
import java.time.format.TextStyle

@Composable
fun HistoryRoute(
    onWorkoutClick: (workoutId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onWorkoutClick = onWorkoutClick,
        modifier = modifier,
        calendarActions =
        CalendarActions(
            onViewChange = viewModel::onViewChange,
            onMonthChange = viewModel::onMonthChange,
            onDayClick = viewModel::onDayClick,
        ),
    )
}

/** What the List / Calendar switch and the calendar do. */
@Immutable
class CalendarActions(
    val onViewChange: (HistoryView) -> Unit,
    val onMonthChange: (offset: Int) -> Unit,
    val onDayClick: (LocalDate) -> Unit,
) {
    companion object {
        val None = CalendarActions({}, {}, {})
    }
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onWorkoutClick: (workoutId: String) -> Unit,
    modifier: Modifier = Modifier,
    calendarActions: CalendarActions = CalendarActions.None,
) {
    when (state) {
        HistoryUiState.Loading ->
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        is HistoryUiState.Loaded ->
            if (state.weeks.isEmpty()) {
                EmptyHistory(modifier)
            } else {
                HistoryContent(state, onWorkoutClick, calendarActions, modifier)
            }
    }
}

@Composable
private fun HistoryContent(
    state: HistoryUiState.Loaded,
    onWorkoutClick: (workoutId: String) -> Unit,
    calendarActions: CalendarActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                Text(text = stringResource(R.string.nav_history), style = MaterialTheme.typography.headlineMedium)
                ViewSwitch(state.view, calendarActions.onViewChange)
            }
        }
        val calendar = state.calendar
        if (state.view == HistoryView.CALENDAR && calendar != null) {
            item(key = "calendar") { MonthCalendar(calendar, calendarActions) }
            item(key = "calendar-heading") { MonthListHeader(calendar) }
            if (calendar.listed.isEmpty()) {
                item(key = "calendar-empty") {
                    Text(
                        text = stringResource(R.string.history_month_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(calendar.listed, key = { it.id }) { workout -> WorkoutCard(workout, { onWorkoutClick(workout.id) }) }
        } else {
            state.weeks.forEach { week ->
                item(key = "week-${week.weekStart}") { WeekHeader(week) }
                items(week.workouts, key = { it.id }) { workout ->
                    WorkoutCard(workout, { onWorkoutClick(workout.id) })
                }
            }
        }
    }
}

@Composable
private fun ViewSwitch(view: HistoryView, onViewChange: (HistoryView) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        HistoryView.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == view,
                onClick = { onViewChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, HistoryView.entries.size),
            ) {
                Text(
                    stringResource(
                        when (option) {
                            HistoryView.LIST -> R.string.history_view_list
                            HistoryView.CALENDAR -> R.string.history_view_calendar
                        },
                    ),
                )
            }
        }
    }
}

/** The month with its weekdays, trained days filled in, today ringed and the picked day outlined. */
@Composable
private fun MonthCalendar(calendar: HistoryMonth, actions: CalendarActions) {
    val locale = LocalConfiguration.current.locales[0]
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { actions.onMonthChange(-1) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.history_month_previous),
                    )
                }
                Text(
                    text = formatDate(calendar.month.atDay(1), "MMMMy"),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { actions.onMonthChange(1) }, enabled = calendar.hasNext) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.history_month_next),
                    )
                }
            }
            Row(modifier = Modifier.padding(top = 4.dp)) {
                weekdays(calendar.firstDayOfWeek).forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clearAndSetSemantics {},
                    )
                }
            }
            calendar.days.chunked(DAYS_IN_WEEK).forEach { week ->
                Row {
                    week.forEach { day ->
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (day != null) {
                                DayCell(
                                    day = day,
                                    count = calendar.workouts[day].orEmpty().size,
                                    isToday = day == calendar.today,
                                    selected = day == calendar.selectedDay,
                                    onClick = { actions.onDayClick(day) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, count: Int, isToday: Boolean, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val trained = count > 0
    val description =
        if (trained) {
            pluralStringResource(R.plurals.history_day_description, count, formatDate(day, "EEEEdMMMM"), count)
        } else {
            formatDate(day, "EEEEdMMMM")
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(3.dp)
            .fillMaxSize()
            .then(if (selected) Modifier.border(2.dp, colors.tertiary, CircleShape) else Modifier)
            .padding(if (selected) 3.dp else 0.dp)
            .clip(CircleShape)
            .then(if (trained) Modifier.background(colors.primary) else Modifier)
            .then(if (isToday && !trained) Modifier.border(1.dp, colors.primary, CircleShape) else Modifier)
            .then(if (trained) Modifier.clickable(onClick = onClick) else Modifier)
            .clearAndSetSemantics {
                contentDescription = description
                this.selected = selected
            },
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday || trained) FontWeight.Bold else null,
            color = if (trained) colors.onPrimary else colors.onSurface,
        )
    }
}

/** The picked day, or the month, with how many workouts and how much volume are listed below. */
@Composable
private fun MonthListHeader(calendar: HistoryMonth) {
    val listed = calendar.listed
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
    ) {
        Text(
            text =
            calendar.selectedDay?.let {
                formatDate(it, "EEEEdMMMM")
            } ?: formatDate(calendar.month.atDay(1), "MMMM"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (listed.isNotEmpty()) {
            Text(
                text =
                stringResource(
                    R.string.history_week_totals,
                    pluralStringResource(R.plurals.history_week_workouts, listed.size, listed.size),
                    formatVolume(listed.sumOf { it.volumeKg }),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val DAYS_IN_WEEK = 7

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
