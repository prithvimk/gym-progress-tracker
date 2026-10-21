package io.github.prithvimk.gymtracker.update

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Release builds have no updater and no network access; updates there come from GitHub Releases. */
@Composable
fun DevUpdateSection(modifier: Modifier = Modifier) {
    // Intentionally empty.
}
