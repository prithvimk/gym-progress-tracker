package io.github.prithvimk.gymtracker.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.ui.theme.GymTrackerTheme

// Placeholder screens for Phase 0. Each moves to its own package as it gets built.

@Composable
fun WorkoutScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(R.string.nav_workout, R.string.placeholder_workout, modifier)
}

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(R.string.nav_history, R.string.placeholder_history, modifier)
}

@Composable
fun ExercisesScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(R.string.nav_exercises, R.string.placeholder_exercises, modifier)
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(R.string.nav_settings, R.string.placeholder_settings, modifier)
}

@Composable
private fun PlaceholderScreen(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutScreenPreview() {
    GymTrackerTheme(dynamicColor = false) {
        WorkoutScreen()
    }
}
