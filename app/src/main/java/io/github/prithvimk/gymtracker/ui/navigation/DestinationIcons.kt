package io.github.prithvimk.gymtracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

val TopLevelDestination.icon: ImageVector
    get() =
        when (this) {
            TopLevelDestination.WORKOUT -> Icons.Filled.PlayArrow
            TopLevelDestination.HISTORY -> Icons.Filled.DateRange
            TopLevelDestination.EXERCISES -> Icons.AutoMirrored.Filled.List
            TopLevelDestination.SETTINGS -> Icons.Filled.Settings
        }
