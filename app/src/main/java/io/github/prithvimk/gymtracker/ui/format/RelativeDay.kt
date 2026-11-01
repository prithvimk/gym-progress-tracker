package io.github.prithvimk.gymtracker.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.ui.history.formatDate
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** A past day as it reads relative to today: "today", "yesterday", a weekday within the week, or a date. */
sealed interface RelativeDay {
    data object Today : RelativeDay

    data object Yesterday : RelativeDay

    /** Two to six days ago, so the weekday alone is unambiguous. */
    data class ThisWeek(val dayOfWeek: DayOfWeek) : RelativeDay

    data class Earlier(val date: LocalDate, val sameYear: Boolean) : RelativeDay
}

/** How [date] reads relative to [today]. A date after today, from a clock change, counts as today. */
fun relativeDay(date: LocalDate, today: LocalDate): RelativeDay {
    val daysAgo = ChronoUnit.DAYS.between(date, today)
    return when {
        daysAgo <= 0 -> RelativeDay.Today
        daysAgo == 1L -> RelativeDay.Yesterday
        daysAgo < DAYS_PER_WEEK -> RelativeDay.ThisWeek(date.dayOfWeek)
        else -> RelativeDay.Earlier(date, sameYear = date.year == today.year)
    }
}

/** When [epochMillis] was, relative to today in the phone's time zone, e.g. "yesterday" or "Tuesday". */
@Composable
fun relativeDayText(epochMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    return when (val day = relativeDay(date, LocalDate.now(zone))) {
        RelativeDay.Today -> stringResource(R.string.relative_day_today)
        RelativeDay.Yesterday -> stringResource(R.string.relative_day_yesterday)
        is RelativeDay.ThisWeek -> formatDate(date, "EEEE")
        is RelativeDay.Earlier -> formatDate(day.date, if (day.sameYear) "dMMM" else "dMMMy")
    }
}

private const val DAYS_PER_WEEK = 7L
