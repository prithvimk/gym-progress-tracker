package io.github.prithvimk.gymtracker.ui.format

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeDayTest {
    private val today = LocalDate.of(2026, 11, 1) // A Sunday.

    @Test
    fun `today and yesterday read as words`() {
        assertEquals(RelativeDay.Today, relativeDay(today, today))
        assertEquals(RelativeDay.Yesterday, relativeDay(today.minusDays(1), today))
    }

    @Test
    fun `days within the week read as their weekday`() {
        assertEquals(RelativeDay.ThisWeek(DayOfWeek.FRIDAY), relativeDay(today.minusDays(2), today))
        assertEquals(RelativeDay.ThisWeek(DayOfWeek.MONDAY), relativeDay(today.minusDays(6), today))
    }

    @Test
    fun `a week or more ago reads as a date, with the year only when it differs`() {
        assertEquals(
            RelativeDay.Earlier(LocalDate.of(2026, 10, 25), sameYear = true),
            relativeDay(today.minusDays(7), today)
        )
        assertEquals(
            RelativeDay.Earlier(LocalDate.of(2025, 12, 30), sameYear = false),
            relativeDay(LocalDate.of(2025, 12, 30), today),
        )
    }

    @Test
    fun `a day after today counts as today`() {
        assertEquals(RelativeDay.Today, relativeDay(today.plusDays(1), today))
    }
}
