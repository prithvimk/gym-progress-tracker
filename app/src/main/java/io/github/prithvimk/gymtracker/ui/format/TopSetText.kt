package io.github.prithvimk.gymtracker.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.domain.TopSet

/** A top set as "82.5 kg × 8", or "12 reps" for a bodyweight set. */
@Composable
fun topSetText(top: TopSet): String = if (top.weightKg > 0) {
    stringResource(R.string.exercise_detail_set, weightText(top.weightKg), top.reps)
} else {
    pluralStringResource(R.plurals.exercise_detail_set_reps, top.reps, top.reps)
}
