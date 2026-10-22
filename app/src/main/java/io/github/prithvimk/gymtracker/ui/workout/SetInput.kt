package io.github.prithvimk.gymtracker.ui.workout

import io.github.prithvimk.gymtracker.domain.model.SetType
import java.math.BigDecimal
import java.math.RoundingMode

/** What a set number field holds: a value to save, or text that is not a valid value yet. */
sealed interface FieldInput<out T> {
    data class Valid<T>(val value: T) : FieldInput<T>

    data object Invalid : FieldInput<Nothing>
}

/** Weight in kg. Blank is 0 (bodyweight); a comma works as the decimal point. */
fun parseWeight(text: String): FieldInput<Double> {
    val trimmed = text.trim().replace(',', '.')
    if (trimmed.isEmpty()) return FieldInput.Valid(0.0)
    val value = trimmed.toDoubleOrNull() ?: return FieldInput.Invalid
    return if (value in 0.0..MAX_WEIGHT_KG) FieldInput.Valid(value) else FieldInput.Invalid
}

/** Whole reps. Blank is 0. */
fun parseReps(text: String): FieldInput<Int> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return FieldInput.Valid(0)
    if (!trimmed.all(Char::isDigit)) return FieldInput.Invalid
    val value = trimmed.toIntOrNull() ?: return FieldInput.Invalid
    return if (value <= MAX_REPS) FieldInput.Valid(value) else FieldInput.Invalid
}

/** RPE from 1 to 10 in half steps. Blank means no RPE. */
fun parseRpe(text: String): FieldInput<Double?> {
    val trimmed = text.trim().replace(',', '.')
    if (trimmed.isEmpty()) return FieldInput.Valid(null)
    val value = trimmed.toDoubleOrNull() ?: return FieldInput.Invalid
    val isHalfStep = (value * 2) % 1.0 == 0.0
    return if (value in MIN_RPE..MAX_RPE && isHalfStep) FieldInput.Valid(value) else FieldInput.Invalid
}

/** Formats a stored number for a field: no trailing zeros, at most two decimals, and blank for zero. */
fun formatSetNumber(value: Double?): String {
    if (value == null || value == 0.0) return ""
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

/** The label in a set's first column: working sets are numbered, other types show their kind. */
sealed interface SetLabel {
    data class Number(val value: Int) : SetLabel

    data class Kind(val type: SetType) : SetLabel
}

fun setLabels(types: List<SetType>): List<SetLabel> {
    var working = 0
    return types.map { type ->
        if (type == SetType.WORKING) SetLabel.Number(++working) else SetLabel.Kind(type)
    }
}

internal const val MAX_WEIGHT_KG = 2_000.0
internal const val MAX_REPS = 999
internal const val MIN_RPE = 1.0
internal const val MAX_RPE = 10.0
