package io.github.prithvimk.gymtracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val appLightColors =
    lightColorScheme(
        primary = Color(0xFF2E6B30),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB0F2A8),
        onPrimaryContainer = Color(0xFF002204),
        secondary = Color(0xFF52634F),
        background = Color(0xFFFBFEFB),
        surface = Color(0xFFFBFEFB),
    )

private val appDarkColors =
    darkColorScheme(
        primary = Color(0xFF95D58E),
        onPrimary = Color(0xFF00390A),
        primaryContainer = Color(0xFF125219),
        onPrimaryContainer = Color(0xFFB0F2A8),
        secondary = Color(0xFFB9CCB4),
        background = Color(0xFF191C19),
        surface = Color(0xFF191C19),
    )

@Composable
fun GymTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> appDarkColors
            else -> appLightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
