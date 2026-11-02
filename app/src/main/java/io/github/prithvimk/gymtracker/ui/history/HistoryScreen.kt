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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.data.db.dao.WorkoutHistoryRow
import io.github.prithvimk.gymtracker.ui.components.RecordChip
import io.github.prithvimk.gymtracker.ui.format.durationText
import io.github.prithvimk.gymtracker.ui.format.volumeText
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme
import io.github.prithvimk.gymtracker.ui.theme.tabular
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
        item(key = "header") { HistoryHeader(state.view, calendarActions.onViewChange) }
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
            items(calendar.listed, key = { it.id }) { workout ->
                WorkoutCard(workout, state.exerciseNames[workout.id].orEmpty(), { onWorkoutClick(workout.id) })
            }
        } else {
            state.weeks.forEach { week ->
                item(key = "week-${week.weekStart}") { WeekHeader(week) }
                items(week.workouts, key = { it.id }) { workout ->
                    WorkoutCard(workout, state.exerciseNames[workout.id].orEmpty(), { onWorkoutClick(workout.id) })
                }
            }
        }
    }
}

/** The tab's title, with an icon that switches between the list and the calendar. */
@Composable
private fun HistoryHeader(view: HistoryView, onViewChange: (HistoryView) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            text = stringResource(R.string.nav_history),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (view == HistoryView.LIST) {
            IconButton(onClick = { onViewChange(HistoryView.CALENDAR) }) {
                Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.history_show_calendar))
            }
        } else {
            IconButton(onClick = { onViewChange(HistoryView.LIST) }) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.history_show_list))
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
                                    level = calendar.volumeLevels[day] ?: 0,
                                    isToday = day == calendar.today,
                                    selected = day == calendar.selectedDay,
                                    onClick = { actions.onDayClick(day) },
                                )
                            }
                        }
                    }
                }
            }
            VolumeLegend(Modifier.align(Alignment.End).padding(top = 4.dp, end = 8.dp, bottom = 4.dp))
        }
    }
}

/** "Less ▢▢▢ More volume": what the tints on trained days mean. */
@Composable
private fun VolumeLegend(modifier: Modifier = Modifier) {
    val less = stringResource(R.string.history_legend_less)
    val more = stringResource(R.string.history_legend_more)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Text(less, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        (1..VOLUME_LEVELS).forEach { level ->
            Box(Modifier.size(12.dp).clip(CircleShape).background(volumeTint(level)))
        }
        Text(more, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * A trained day's fill by volume [level]: half-way to the primary container, the container itself,
 * then half-way on to primary, which is kept for the picked day.
 */
@Composable
private fun volumeTint(level: Int): Color {
    val colors = MaterialTheme.colorScheme
    return when (level) {
        1 -> lerp(colors.surfaceContainerHigh, colors.primaryContainer, HALF)
        2 -> colors.primaryContainer
        else -> lerp(colors.primaryContainer, colors.primary, HALF)
    }
}

/** Dark or light text, whichever reads better on [background]. */
@Composable
private fun contentOn(background: Color): Color {
    val colors = MaterialTheme.colorScheme
    val dark = if (colors.onSurface.luminance() < colors.surface.luminance()) colors.onSurface else colors.surface
    val light = if (dark == colors.onSurface) colors.surface else colors.onSurface
    return if (background.luminance() > LIGHT_BACKGROUND_LUMINANCE) dark else light
}

private const val HALF = 0.5f
private const val LIGHT_BACKGROUND_LUMINANCE = 0.3f

/**
 * One day of the month. A trained day is tinted by its volume [level] and can be picked; the picked
 * day is filled with primary, and today keeps its ring whatever else it is.
 */
@Composable
private fun DayCell(day: LocalDate, count: Int, level: Int, isToday: Boolean, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val trained = count > 0
    val description =
        if (trained) {
            pluralStringResource(R.plurals.history_day_description, count, formatDate(day, "EEEEdMMMM"), count)
        } else {
            formatDate(day, "EEEEdMMMM")
        }
    val fill =
        when {
            selected -> colors.primary
            trained -> volumeTint(level)
            else -> Color.Transparent
        }
    val textColor =
        when {
            selected -> colors.onPrimary
            trained -> contentOn(fill)
            else -> colors.onSurface
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(2.dp)
            .fillMaxSize()
            .then(if (isToday) Modifier.border(2.dp, colors.primary, CircleShape) else Modifier)
            .padding(4.dp)
            .clip(CircleShape)
            .background(fill)
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
            color = textColor,
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
                    volumeText(listed.sumOf { it.volumeKg }),
                ),
                style = MaterialTheme.typography.bodyMedium.tabular(),
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
                volumeText(week.volumeKg),
            ),
            style = MaterialTheme.typography.bodyMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A finished workout: when it started, with a gold chip if it set records, its routine, its duration,
 * sets and volume, and the first few of its [exerciseNames].
 */
@Composable
internal fun WorkoutCard(
    workout: WorkoutHistoryRow,
    exerciseNames: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 28.dp)) {
                Text(
                    text = formatStartTime(workout.startedAt),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (workout.recordCount > 0) {
                    RecordChip(
                        pluralStringResource(
                            R.plurals.history_workout_records,
                            workout.recordCount,
                            workout.recordCount
                        ),
                    )
                }
            }
            Text(
                text = workout.routineName ?: stringResource(R.string.history_workout_untitled),
                style = MaterialTheme.typography.titleLarge,
            )
            WorkoutStats(workout)
            if (exerciseNames.isNotEmpty()) {
                Text(
                    text = exercisesText(exerciseNames),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Duration with a timer icon, then sets and volume, on one row. */
@Composable
private fun WorkoutStats(workout: WorkoutHistoryRow) {
    val style = MaterialTheme.typography.bodyLarge.tabular()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_rest_timer),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                durationText(workout.endedAt - workout.startedAt),
                style = style,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Text(pluralStringResource(R.plurals.history_workout_sets, workout.setCount, workout.setCount), style = style)
        Text(volumeText(workout.volumeKg), style = style)
    }
}

/** The first [CARD_EXERCISES] names, then how many more: "Bench Press, Overhead Press, Dip +2". */
@Composable
private fun exercisesText(names: List<String>): String {
    val shown = names.take(CARD_EXERCISES).joinToString()
    val more = names.size - CARD_EXERCISES
    return if (more > 0) stringResource(R.string.history_workout_more_exercises, shown, more) else shown
}

private const val CARD_EXERCISES = 3

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
