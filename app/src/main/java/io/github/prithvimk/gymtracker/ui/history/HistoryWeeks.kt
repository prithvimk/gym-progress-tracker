package io.github.prithvimk.gymtracker.ui.history

import io.github.prithvimk.gymtracker.data.db.dao.WorkoutHistoryRow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** How a week's header reads: relative for the two most recent weeks, otherwise by its first day. */
sealed interface WeekLabel {
    data object ThisWeek : WeekLabel

    data object LastWeek : WeekLabel

    /** [sameYear] is whether the week starts in the current year, so the year can be left out. */
    data class StartingOn(val date: LocalDate, val sameYear: Boolean) : WeekLabel
}

/** One week of finished workouts, newest first, with its totals for the header. */
data class HistoryWeek(val label: WeekLabel, val weekStart: LocalDate, val workouts: List<WorkoutHistoryRow>) {
    val volumeKg: Double get() = workouts.sumOf { it.volumeKg }
}

/**
 * Groups [workouts] (newest first) into weeks starting on [firstDayOfWeek], by the day each started in
 * [zone]. Weeks with no workouts are left out. [today] decides which weeks read "This week" and "Last week".
 */
fun groupByWeek(
    workouts: List<WorkoutHistoryRow>,
    zone: ZoneId,
    firstDayOfWeek: DayOfWeek,
    today: LocalDate,
): List<HistoryWeek> {
    fun weekStart(date: LocalDate) = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val thisWeek = weekStart(today)
    return workouts
        .sortedByDescending { it.startedAt }
        .groupBy { weekStart(Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate()) }
        .map { (start, inWeek) ->
            val label =
                when (start) {
                    thisWeek -> WeekLabel.ThisWeek
                    thisWeek.minusWeeks(1) -> WeekLabel.LastWeek
                    else -> WeekLabel.StartingOn(start, sameYear = start.year == today.year)
                }
            HistoryWeek(label, start, inWeek)
        }
}
