package io.github.prithvimk.gymtracker.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import io.github.prithvimk.gymtracker.R
import java.text.NumberFormat
import java.util.Locale

// The app's one formatter for weights and durations. Every weight shown outside the set fields goes
// through here, so a display unit other than kg is a change to this file alone.

/**
 * A live timer as "m:ss", or "h:mm:ss" from one hour. Negative input counts as zero. Only running
 * clocks (the workout's elapsed time, the rest countdown) and rest settings use this; a finished
 * duration reads in words, see [durationText].
 */
fun formatClock(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / MILLIS_PER_SECOND
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** A finished duration in whole hours and minutes, rounded to the nearest minute. */
data class DurationParts(val hours: Long, val minutes: Long)

fun durationParts(millis: Long): DurationParts {
    val totalMinutes = (millis.coerceAtLeast(0L) + MILLIS_PER_MINUTE / 2) / MILLIS_PER_MINUTE
    return DurationParts(totalMinutes / MINUTES_PER_HOUR, totalMinutes % MINUTES_PER_HOUR)
}

/** A finished duration in words: "58 min", "1 h 11 min", or "2 h" on the hour. */
@Composable
@ReadOnlyComposable
fun durationText(millis: Long): String {
    val (hours, minutes) = durationParts(millis)
    return when {
        hours == 0L -> stringResource(R.string.duration_minutes, minutes)
        minutes == 0L -> stringResource(R.string.duration_hours, hours)
        else -> stringResource(R.string.duration_hours_minutes, hours, minutes)
    }
}

/** A weight's number in the display unit, grouped, with at most two decimals: "82.5", "1,000", "1.25". */
fun formatWeight(kg: Double, locale: Locale = Locale.getDefault()): String = numberFormat(locale, 2).format(kg)

/** A volume's or estimate's number in the display unit, grouped, with at most one decimal: "12,345.5". */
fun formatVolume(kg: Double, locale: Locale = Locale.getDefault()): String = numberFormat(locale, 1).format(kg)

/** A weight with its unit, e.g. "82.5 kg". */
@Composable
@ReadOnlyComposable
fun weightText(kg: Double): String = stringResource(R.string.weight_value, formatWeight(kg, currentLocale()))

/** A volume or estimated 1RM with its unit, e.g. "8,420 kg". */
@Composable
@ReadOnlyComposable
fun volumeText(kg: Double): String = stringResource(R.string.weight_value, formatVolume(kg, currentLocale()))

/** A weight's number alone, for compact columns where the unit is in the header, e.g. "80" of "80 × 8". */
@Composable
@ReadOnlyComposable
fun weightNumberText(kg: Double): String = formatWeight(kg, currentLocale())

@Composable
@ReadOnlyComposable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

private fun numberFormat(locale: Locale, maxDecimals: Int): NumberFormat =
    NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = maxDecimals }

private const val MILLIS_PER_SECOND = 1_000L
private const val MILLIS_PER_MINUTE = 60_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val MINUTES_PER_HOUR = 60L
