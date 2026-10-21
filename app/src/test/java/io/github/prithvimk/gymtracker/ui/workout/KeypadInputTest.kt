package io.github.prithvimk.gymtracker.ui.workout

import io.github.prithvimk.gymtracker.data.workoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeypadInputTest {
    private fun keypad(field: SetField, text: String, replaceOnType: Boolean = true) =
        KeypadState(SetFieldRef("s", field), text, replaceOnType)

    private fun KeypadState.type(keys: String): KeypadState = keys.fold(this) { state, key ->
        state.press(
            when (key) {
                '.' -> KeypadKey.Decimal
                '<' -> KeypadKey.Backspace
                else -> KeypadKey.Digit(key)
            },
        )
    }

    @Test
    fun `typing over a freshly opened value starts it over`() {
        assertEquals("82.5", keypad(SetField.WEIGHT, "60").type("82.5").text)
    }

    @Test
    fun `once typing has started keys append`() {
        val state = keypad(SetField.WEIGHT, "60").type("8")

        assertFalse(state.replaceOnType)
        assertEquals("85", state.type("5").text)
    }

    @Test
    fun `backspace on a fresh value clears it, then deletes one character`() {
        assertEquals("", keypad(SetField.REPS, "8").type("<").text)
        assertEquals("1", keypad(SetField.REPS, "12", replaceOnType = false).type("<").text)
    }

    @Test
    fun `a decimal point starts with a zero and only goes in once`() {
        assertEquals("0.5", keypad(SetField.WEIGHT, "60").type(".5").text)
        assertEquals("2.5", keypad(SetField.WEIGHT, "").type("2..5").text)
    }

    @Test
    fun `reps take no decimal point, and pressing one changes nothing`() {
        val fresh = keypad(SetField.REPS, "8")

        assertEquals(fresh, fresh.press(KeypadKey.Decimal))
    }

    @Test
    fun `a leading zero gives way to the next digit`() {
        assertEquals("5", keypad(SetField.REPS, "").type("05").text)
    }

    @Test
    fun `text stops growing at seven characters`() {
        assertEquals("1234.56", keypad(SetField.WEIGHT, "").type("1234.567").text)
    }

    @Test
    fun `weight steps by 2_5 kg and stops at zero`() {
        assertEquals("62.5", keypad(SetField.WEIGHT, "60").stepped(up = true).text)
        assertEquals("57.5", keypad(SetField.WEIGHT, "60").stepped(up = false).text)
        assertEquals("", keypad(SetField.WEIGHT, "2").stepped(up = false).text)
        assertEquals("2.5", keypad(SetField.WEIGHT, "").stepped(up = true).text)
    }

    @Test
    fun `reps step by one and stop at zero`() {
        assertEquals("9", keypad(SetField.REPS, "8").stepped(up = true).text)
        assertEquals("", keypad(SetField.REPS, "1").stepped(up = false).text)
        assertEquals("", keypad(SetField.REPS, "").stepped(up = false).text)
    }

    @Test
    fun `RPE steps by a half within 1 to 10, starting blank at 8`() {
        assertEquals("8.5", keypad(SetField.RPE, "8").stepped(up = true).text)
        assertEquals("10", keypad(SetField.RPE, "10").stepped(up = true).text)
        assertEquals("1", keypad(SetField.RPE, "1").stepped(up = false).text)
        assertEquals("8", keypad(SetField.RPE, "").stepped(up = true).text)
        assertEquals("8", keypad(SetField.RPE, "").stepped(up = false).text)
    }

    @Test
    fun `stepping leaves the value ready to be typed over`() {
        val stepped = keypad(SetField.WEIGHT, "60", replaceOnType = false).stepped(up = true)

        assertTrue(stepped.replaceOnType)
        assertEquals("7", stepped.type("7").text)
    }

    @Test
    fun `text that is not a value yet steps from zero`() {
        assertEquals("2.5", keypad(SetField.WEIGHT, ".", replaceOnType = false).stepped(up = true).text)
    }

    @Test
    fun `validity follows what the field accepts`() {
        assertTrue(keypad(SetField.WEIGHT, "62.").isValid)
        assertTrue(keypad(SetField.RPE, "").isValid)
        assertFalse(keypad(SetField.RPE, "8.3").isValid)
        assertFalse(keypad(SetField.WEIGHT, "1999.9.").isValid)
    }

    @Test
    fun `next goes from weight to reps, then to the next set's weight`() {
        val ids = listOf("a", "b")

        assertEquals(SetFieldRef("a", SetField.REPS), nextField(ids, SetFieldRef("a", SetField.WEIGHT)))
        assertEquals(SetFieldRef("b", SetField.WEIGHT), nextField(ids, SetFieldRef("a", SetField.REPS)))
        assertEquals(SetFieldRef("b", SetField.WEIGHT), nextField(ids, SetFieldRef("a", SetField.RPE)))
    }

    @Test
    fun `next after the last set closes the keypad`() {
        assertNull(nextField(listOf("a", "b"), SetFieldRef("b", SetField.REPS)))
        assertNull(nextField(listOf("a"), SetFieldRef("gone", SetField.REPS)))
    }

    @Test
    fun `stored values show as the logger shows them`() {
        val set = workoutSet("s", "we", weightKg = 62.5, reps = 0)

        assertEquals("62.5", set.fieldText(SetField.WEIGHT))
        assertEquals("", set.fieldText(SetField.REPS))
        assertEquals("", set.fieldText(SetField.RPE))
    }
}
