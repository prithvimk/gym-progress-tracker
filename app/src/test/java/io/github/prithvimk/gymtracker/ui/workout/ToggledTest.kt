package io.github.prithvimk.gymtracker.ui.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class ToggledTest {
    @Test
    fun `ticking keeps tap order`() {
        assertEquals(listOf("ohp", "bench"), emptyList<String>().toggled("ohp").toggled("bench"))
    }

    @Test
    fun `unticking removes and re-ticking goes to the end`() {
        val selection = listOf("ohp", "bench", "fly").toggled("ohp")
        assertEquals(listOf("bench", "fly"), selection)
        assertEquals(listOf("bench", "fly", "ohp"), selection.toggled("ohp"))
    }
}
