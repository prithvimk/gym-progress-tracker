package io.github.prithvimk.gymtracker.ui.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatElapsedTest {
    @Test
    fun `under an hour shows minutes and seconds`() {
        assertEquals("0:00", formatElapsed(0L))
        assertEquals("0:59", formatElapsed(59_999L))
        assertEquals("12:34", formatElapsed(754_000L))
    }

    @Test
    fun `an hour or more adds the hour`() {
        assertEquals("1:00:00", formatElapsed(3_600_000L))
        assertEquals("1:05:09", formatElapsed(3_909_000L))
    }

    @Test
    fun `negative time counts as zero`() {
        assertEquals("0:00", formatElapsed(-5_000L))
    }
}
