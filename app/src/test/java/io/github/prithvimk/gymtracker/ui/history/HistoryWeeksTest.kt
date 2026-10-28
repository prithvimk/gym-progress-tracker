package io.github.prithvimk.gymtracker.ui.history

import io.github.prithvimk.gymtracker.data.db.dao.WorkoutHistoryRow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryWeeksTest {
    private val zone = ZoneId.of("Europe/London")

    // A Thursday.
    private val today = LocalDate.of(2026, 10, 22)

    @Test
    fun `workouts group into weeks, newest first, labelled this week, last week and by start day`() {
        val weeks =
            groupByWeek(
                listOf(
                    workout("a", LocalDateTime.of(2026, 10, 21, 18, 0)),
                    workout("b", LocalDateTime.of(2026, 10, 19, 7, 0)),
                    workout("c", LocalDateTime.of(2026, 10, 16, 18, 0)),
                    workout("d", LocalDateTime.of(2026, 9, 30, 18, 0)),
                ),
                zone,
                DayOfWeek.MONDAY,
                today,
            )

        assertEquals(
            listOf(
                WeekLabel.ThisWeek,
                WeekLabel.LastWeek,
                WeekLabel.StartingOn(LocalDate.of(2026, 9, 28), sameYear = true),
            ),
            weeks.map { it.label },
        )
        assertEquals(
            listOf(listOf("a", "b"), listOf("c"), listOf("d")),
            weeks.map { week ->
                week.workouts.map { it.id }
            }
        )
    }

    @Test
    fun `the week starts on the given day`() {
        val sunday = workout("sun", LocalDateTime.of(2026, 10, 18, 10, 0))

        assertEquals(WeekLabel.LastWeek, groupByWeek(listOf(sunday), zone, DayOfWeek.MONDAY, today).single().label)
        assertEquals(WeekLabel.ThisWeek, groupByWeek(listOf(sunday), zone, DayOfWeek.SUNDAY, today).single().label)
    }

    @Test
    fun `a workout belongs to the day it started in the given zone`() {
        // 23:30 on Sunday in London is already Monday in Tokyo.
        val late = workout("late", LocalDateTime.of(2026, 10, 18, 23, 30))

        assertEquals(WeekLabel.LastWeek, groupByWeek(listOf(late), zone, DayOfWeek.MONDAY, today).single().label)
        assertEquals(
            WeekLabel.ThisWeek,
            groupByWeek(listOf(late), ZoneId.of("Asia/Tokyo"), DayOfWeek.MONDAY, today).single().label,
        )
    }

    @Test
    fun `weeks from another year say so`() {
        val old = workout("old", LocalDateTime.of(2025, 12, 31, 18, 0))

        assertEquals(
            WeekLabel.StartingOn(LocalDate.of(2025, 12, 29), sameYear = false),
            groupByWeek(listOf(old), zone, DayOfWeek.MONDAY, today).single().label,
        )
    }

    @Test
    fun `a week's volume adds up its workouts`() {
        val weeks =
            groupByWeek(
                listOf(
                    workout("a", LocalDateTime.of(2026, 10, 21, 18, 0), volumeKg = 1_000.0),
                    workout("b", LocalDateTime.of(2026, 10, 20, 18, 0), volumeKg = 250.5),
                ),
                zone,
                DayOfWeek.MONDAY,
                today,
            )

        assertEquals(1_250.5, weeks.single().volumeKg, 1e-9)
    }

    @Test
    fun `no workouts is no weeks`() {
        assertEquals(emptyList<HistoryWeek>(), groupByWeek(emptyList(), zone, DayOfWeek.MONDAY, today))
    }

    private fun workout(id: String, start: LocalDateTime, volumeKg: Double = 0.0): WorkoutHistoryRow {
        val startedAt = start.atZone(zone).toInstant().toEpochMilli()
        return WorkoutHistoryRow(
            id,
            null,
            startedAt,
            startedAt + 3_600_000L,
            setCount = 0,
            volumeKg = volumeKg,
            recordCount = 0
        )
    }
}
