package io.github.prithvimk.gymtracker.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatClockTest {
    @Test
    fun `under an hour shows minutes and seconds`() {
        assertEquals("0:00", formatClock(0L))
        assertEquals("0:59", formatClock(59_999L))
        assertEquals("12:34", formatClock(754_000L))
    }

    @Test
    fun `an hour or more adds the hour`() {
        assertEquals("1:00:00", formatClock(3_600_000L))
        assertEquals("1:05:09", formatClock(3_909_000L))
    }

    @Test
    fun `negative time counts as zero`() {
        assertEquals("0:00", formatClock(-5_000L))
    }

    @Test
    fun `durations round to the nearest minute`() {
        assertEquals(DurationParts(0, 58), durationParts(58 * 60_000L))
        assertEquals(DurationParts(0, 58), durationParts(58 * 60_000L + 29_999L))
        assertEquals(DurationParts(0, 59), durationParts(58 * 60_000L + 30_000L))
        assertEquals(DurationParts(0, 0), durationParts(20_000L))
    }

    @Test
    fun `durations of an hour or more split into hours and minutes`() {
        assertEquals(DurationParts(1, 11), durationParts(71 * 60_000L))
        assertEquals(DurationParts(1, 0), durationParts(59 * 60_000L + 45_000L))
        assertEquals(DurationParts(2, 0), durationParts(2 * 3_600_000L))
    }

    @Test
    fun `negative durations count as zero`() {
        assertEquals(DurationParts(0, 0), durationParts(-60_000L))
    }
}
