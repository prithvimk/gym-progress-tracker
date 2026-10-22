package io.github.prithvimk.gymtracker.ui.workout

import io.github.prithvimk.gymtracker.data.db.entity.WorkoutSet

/** A number field of a set that the keypad can type into. */
enum class SetField {
    WEIGHT,
    REPS,
    RPE,
}

data class SetFieldRef(val setId: String, val field: SetField)

/** A keypad key that edits the text. */
sealed interface KeypadKey {
    data class Digit(val digit: Char) : KeypadKey

    data object Decimal : KeypadKey

    data object Backspace : KeypadKey
}

/**
 * The keypad's edit of one field. While [replaceOnType] is set (the field was just opened or
 * stepped) the next key starts the text over, so a prefilled 60 can be typed over without
 * clearing it first.
 */
data class KeypadState(val target: SetFieldRef, val text: String, val replaceOnType: Boolean = true) {
    val field: SetField get() = target.field

    /** Whether [text] is a value the field accepts, and so has been saved. */
    val isValid: Boolean
        get() {
            // Inside a getter a bare `field` is the property's own backing field, so this names the target's.
            val input =
                when (target.field) {
                    SetField.WEIGHT -> parseWeight(text)
                    SetField.REPS -> parseReps(text)
                    SetField.RPE -> parseRpe(text)
                }
            return input is FieldInput.Valid
        }
}

fun KeypadState.press(key: KeypadKey): KeypadState {
    val base = if (replaceOnType) "" else text
    val next =
        when (key) {
            is KeypadKey.Digit ->
                when {
                    base == "0" -> key.digit.toString()
                    base.length >= MAX_TEXT_LENGTH -> base
                    else -> base + key.digit
                }
            // Reps are whole, and a number takes one decimal point.
            KeypadKey.Decimal ->
                when {
                    field == SetField.REPS || '.' in base -> return this
                    base.isEmpty() -> "0."
                    else -> "$base."
                }
            KeypadKey.Backspace -> base.dropLast(1)
        }
    return copy(text = next, replaceOnType = false)
}

/**
 * Steps the value one [stepSize] up or down, within what the field accepts. Text that is not a
 * value yet steps from zero; a blank RPE goes to [RPE_START] either way.
 */
fun KeypadState.stepped(up: Boolean): KeypadState {
    val sign = if (up) 1 else -1
    val next =
        when (field) {
            SetField.WEIGHT -> {
                val value = parseWeight(text).valueOrNull() ?: 0.0
                formatSetNumber((value + sign * WEIGHT_STEP_KG).coerceIn(0.0, MAX_WEIGHT_KG))
            }
            SetField.REPS -> {
                val value = parseReps(text).valueOrNull() ?: 0
                val reps = (value + sign).coerceIn(0, MAX_REPS)
                if (reps == 0) "" else reps.toString()
            }
            SetField.RPE -> {
                val value = parseRpe(text).valueOrNull()
                formatSetNumber(value?.let { (it + sign * RPE_STEP).coerceIn(MIN_RPE, MAX_RPE) } ?: RPE_START)
            }
        }
    return copy(text = next, replaceOnType = true)
}

/** How far one press of a stepper moves the field. */
val SetField.stepSize: Double
    get() =
        when (this) {
            SetField.WEIGHT -> WEIGHT_STEP_KG
            SetField.REPS -> 1.0
            SetField.RPE -> RPE_STEP
        }

/**
 * Where Next goes from [from], given the workout's set ids in screen order: weight to reps of the
 * same set, then the next set's weight. RPE is optional, so Next passes it by. Null after the last set.
 */
fun nextField(setIds: List<String>, from: SetFieldRef): SetFieldRef? {
    if (from.field == SetField.WEIGHT) return from.copy(field = SetField.REPS)
    val index = setIds.indexOf(from.setId)
    if (index < 0) return null
    val nextSetId = setIds.getOrNull(index + 1) ?: return null
    return SetFieldRef(nextSetId, SetField.WEIGHT)
}

/** The field's stored value as the logger shows it: blank for no weight, no reps or no RPE. */
fun WorkoutSet.fieldText(field: SetField): String = when (field) {
    SetField.WEIGHT -> formatSetNumber(weightKg)
    SetField.REPS -> if (reps == 0) "" else reps.toString()
    SetField.RPE -> formatSetNumber(rpe)
}

private fun <T> FieldInput<T>.valueOrNull(): T? = when (this) {
    is FieldInput.Valid -> value
    FieldInput.Invalid -> null
}

private const val MAX_TEXT_LENGTH = 7
private const val WEIGHT_STEP_KG = 2.5
private const val RPE_STEP = 0.5
private const val RPE_START = 8.0
