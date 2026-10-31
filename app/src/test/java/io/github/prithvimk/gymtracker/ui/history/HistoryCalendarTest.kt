package io.github.prithvimk.gymtracker.ui.history

import io.github.prithvimk.gymtracker.data.db.dao.WorkoutHistoryRow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryCalendarTest {
    private val zone = ZoneId.of("Europe/London")
    private val october = YearMonth.of(2026, 10)

    @Test
    fun `the grid pads October 2026 to whole weeks from Monday`() {
        // 1 October 2026 is a Thursday, and the 31st a Saturday.
        val grid = monthGrid(october, DayOfWeek.MONDAY)

        assertEquals(35, grid.size)
        assertEquals(listOf(null, null, null), grid.take(3))
        assertEquals(LocalDate.of(2026, 10, 1), grid[3])
        assertEquals(LocalDate.of(2026, 10, 31), grid[33])
        assertNull(grid[34])
        assertEquals(31, grid.filterNotNull().size)
    }

    @Test
    fun `the grid follows the week start`() {
        val grid = monthGrid(october, DayOfWeek.SUNDAY)

        assertEquals(4, grid.indexOf(LocalDate.of(2026, 10, 1)))
        assertEquals(35, grid.size)
        assertEquals(DayOfWeek.SUNDAY, weekdays(DayOfWeek.SUNDAY).first())
        assertEquals(DayOfWeek.SATURDAY, weekdays(DayOfWeek.SUNDAY).last())
    }

    @Test
    fun `a month starting on the week start has no leading blanks`() {
        // 1 February 2027 is a Monday and February has 28 days: exactly four weeks.
        val grid = monthGrid(YearMonth.of(2027, 2), DayOfWeek.MONDAY)

        assertEquals(28, grid.size)
        assertEquals(LocalDate.of(2027, 2, 1), grid.first())
        assertEquals(LocalDate.of(2027, 2, 28), grid.last())
    }

    @Test
    fun `workouts go on the day they started in the zone, only for the month, newest first`() {
        val rows =
            listOf(
                workout("late", LocalDateTime.of(2026, 10, 21, 18, 0)),
                workout("early", LocalDateTime.of(2026, 10, 21, 7, 0)),
                // Late on the last day of the month still counts towards it.
                workout("halloween", LocalDateTime.of(2026, 10, 31, 23, 30)),
                workout("september", LocalDateTime.of(2026, 9, 30, 18, 0)),
            )

        val byDay = workoutsByDay(rows, october, zone)

        assertEquals(setOf(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 31)), byDay.keys)
        assertEquals(listOf("late", "early"), byDay.getValue(LocalDate.of(2026, 10, 21)).map { it.id })
    }

    @Test
    fun `the list shows the picked day, or the whole month newest first`() {
        val rows =
            listOf(
                workout("a", LocalDateTime.of(2026, 10, 5, 18, 0)),
                workout("b", LocalDateTime.of(2026, 10, 21, 18, 0)),
            )
        val month = month(rows, selectedDay = null, today = LocalDate.of(2026, 10, 31))

        assertEquals(listOf("b", "a"), month.listed.map { it.id })
        assertEquals(listOf("a"), month.copy(selectedDay = LocalDate.of(2026, 10, 5)).listed.map { it.id })
        assertEquals(emptyList<String>(), month.copy(selectedDay = LocalDate.of(2026, 10, 6)).listed.map { it.id })
    }

    @Test
    fun `months up to the current one can be moved forward to`() {
        assertFalse(month(emptyList(), null, today = LocalDate.of(2026, 10, 31)).hasNext)
        assertTrue(month(emptyList(), null, today = LocalDate.of(2026, 11, 1)).hasNext)
    }

    private fun month(rows: List<WorkoutHistoryRow>, selectedDay: LocalDate?, today: LocalDate) = HistoryMonth(
        month = october,
        firstDayOfWeek = DayOfWeek.MONDAY,
        days = monthGrid(october, DayOfWeek.MONDAY),
        workouts = workoutsByDay(rows, october, zone),
        today = today,
        selectedDay = selectedDay,
    )

    private fun workout(id: String, start: LocalDateTime): WorkoutHistoryRow {
        val startedAt = start.atZone(zone).toInstant().toEpochMilli()
        return WorkoutHistoryRow(id, null, startedAt, startedAt + 3_600_000L, 10, 1_000.0, 0)
    }
}
