package io.github.prithvimk.gymtracker.ui.workout

import io.github.prithvimk.gymtracker.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Test

class SetInputTest {
    @Test
    fun `weight accepts decimals, commas and blank`() {
        assertEquals(FieldInput.Valid(62.5), parseWeight("62.5"))
        assertEquals(FieldInput.Valid(62.5), parseWeight(" 62,5 "))
        assertEquals(FieldInput.Valid(0.0), parseWeight(""))
        assertEquals(FieldInput.Valid(100.0), parseWeight("100"))
    }

    @Test
    fun `weight rejects junk, negatives and absurd values`() {
        assertEquals(FieldInput.Invalid, parseWeight("abc"))
        assertEquals(FieldInput.Invalid, parseWeight("-5"))
        assertEquals(FieldInput.Invalid, parseWeight("1.2.3"))
        assertEquals(FieldInput.Invalid, parseWeight("5000"))
    }

    @Test
    fun `reps accept whole numbers and blank only`() {
        assertEquals(FieldInput.Valid(8), parseReps("8"))
        assertEquals(FieldInput.Valid(0), parseReps(""))
        assertEquals(FieldInput.Invalid, parseReps("8.5"))
        assertEquals(FieldInput.Invalid, parseReps("-1"))
        assertEquals(FieldInput.Invalid, parseReps("1000"))
    }

    @Test
    fun `rpe is optional and goes from 1 to 10 in half steps`() {
        assertEquals(FieldInput.Valid(null), parseRpe(""))
        assertEquals(FieldInput.Valid(8.5), parseRpe("8,5"))
        assertEquals(FieldInput.Valid(10.0), parseRpe("10"))
        assertEquals(FieldInput.Invalid, parseRpe("8.3"))
        assertEquals(FieldInput.Invalid, parseRpe("0"))
        assertEquals(FieldInput.Invalid, parseRpe("11"))
    }

    @Test
    fun `numbers format without trailing zeros and blank for zero`() {
        assertEquals("60", formatSetNumber(60.0))
        assertEquals("62.5", formatSetNumber(62.5))
        assertEquals("1.25", formatSetNumber(1.25))
        assertEquals("", formatSetNumber(0.0))
        assertEquals("", formatSetNumber(null))
    }

    @Test
    fun `working sets are numbered and other types show their kind`() {
        val labels =
            setLabels(listOf(SetType.WARMUP, SetType.WORKING, SetType.WORKING, SetType.DROP, SetType.WORKING))
        assertEquals(
            listOf(
                SetLabel.Kind(SetType.WARMUP),
                SetLabel.Number(1),
                SetLabel.Number(2),
                SetLabel.Kind(SetType.DROP),
                SetLabel.Number(3),
            ),
            labels,
        )
    }
}
