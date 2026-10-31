package io.github.prithvimk.gymtracker.ui.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatNumbersTest {
    @Test
    fun `whole volumes have no decimals and are grouped`() {
        assertEquals("0", formatVolume(0.0, Locale.US))
        assertEquals("12,345", formatVolume(12_345.0, Locale.US))
    }

    @Test
    fun `volumes keep one decimal`() {
        assertEquals("1,532.5", formatVolume(1_532.5, Locale.US))
        assertEquals("100", formatVolume(100.04, Locale.US))
        assertEquals("104.5", formatVolume(104.5333, Locale.US))
    }

    @Test
    fun `weights keep up to two decimals`() {
        assertEquals("82.5", formatWeight(82.5, Locale.US))
        assertEquals("1.25", formatWeight(1.25, Locale.US))
        assertEquals("0", formatWeight(0.0, Locale.US))
        assertEquals("1,000", formatWeight(1_000.0, Locale.US))
    }

    @Test
    fun `grouping and decimal marks follow the locale`() {
        assertEquals("12.345,5", formatVolume(12_345.5, Locale.GERMANY))
        assertEquals("82,5", formatWeight(82.5, Locale.GERMANY))
    }
}
