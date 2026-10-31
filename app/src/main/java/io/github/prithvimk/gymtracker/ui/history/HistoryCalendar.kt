package io.github.prithvimk.gymtracker.ui.history

import io.github.prithvimk.gymtracker.data.db.dao.WorkoutHistoryRow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * One month of the History calendar. [days] fills whole weeks starting on [firstDayOfWeek], with
 * null for the days before the 1st and after the last. [workouts] holds the month's finished workouts
 * by the day they started, newest first. [selectedDay] narrows the list under the calendar to one day.
 */
data class HistoryMonth(
    val month: YearMonth,
    val firstDayOfWeek: DayOfWeek,
    val days: List<LocalDate?>,
    val workouts: Map<LocalDate, List<WorkoutHistoryRow>>,
    val today: LocalDate,
    val selectedDay: LocalDate?,
) {
    /** The workouts listed under the calendar: the selected day's, or else the whole month's, newest first. */
    val listed: List<WorkoutHistoryRow>
        get() = selectedDay?.let { workouts[it].orEmpty() }
            ?: workouts.values.flatten().sortedByDescending { it.startedAt }

    /** Whether a later month can be shown: not past the current one. */
    val hasNext: Boolean get() = month < YearMonth.from(today)
}

/** The days of [month] laid out in weeks starting on [firstDayOfWeek], padded with null to whole weeks. */
fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - firstDayOfWeek.value + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val days = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val trailing = (DAYS_IN_WEEK - days.size % DAYS_IN_WEEK) % DAYS_IN_WEEK
    return days + List(trailing) { null }
}

/** [workouts] started in [month], by the day each started in [zone], newest first within a day. */
fun workoutsByDay(
    workouts: List<WorkoutHistoryRow>,
    month: YearMonth,
    zone: ZoneId
): Map<LocalDate, List<WorkoutHistoryRow>> = workouts
    .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() to it }
    .filter { (day, _) -> YearMonth.from(day) == month }
    .groupBy({ it.first }, { it.second })
    .mapValues { (_, rows) -> rows.sortedByDescending { it.startedAt } }

/** The weekdays in calendar order from [firstDayOfWeek], for the header row. */
fun weekdays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> = List(DAYS_IN_WEEK) { firstDayOfWeek.plus(it.toLong()) }

private const val DAYS_IN_WEEK = 7
