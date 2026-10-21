package io.github.prithvimk.gymtracker.ui.workout

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatVolumeTest {
    @Test
    fun `whole volumes have no decimals and are grouped`() {
        assertEquals("0", formatVolume(0.0, Locale.US))
        assertEquals("12,345", formatVolume(12_345.0, Locale.US))
    }

    @Test
    fun `fractions keep one decimal`() {
        assertEquals("1,532.5", formatVolume(1_532.5, Locale.US))
        assertEquals("100", formatVolume(100.04, Locale.US))
    }

    @Test
    fun `grouping and decimal marks follow the locale`() {
        assertEquals("12.345,5", formatVolume(12_345.5, Locale.GERMANY))
    }
}
