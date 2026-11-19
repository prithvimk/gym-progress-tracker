package io.github.prithvimk.gymtracker.ui.screens

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
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.ui.components.TabScaffold
import io.github.prithvimk.gymtracker.update.DevUpdateSection

// Placeholder screen for Phase 0. It moves to its own package when Settings gets built.

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    TabScaffold(title = stringResource(R.string.nav_settings), modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Debug builds show the dev build updater here; release builds show nothing.
            DevUpdateSection(modifier = Modifier.padding(horizontal = 16.dp))
            Column(
                modifier = Modifier.weight(1f).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.placeholder_settings),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
