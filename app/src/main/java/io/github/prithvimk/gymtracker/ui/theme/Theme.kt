package io.github.prithvimk.gymtracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

// Generated from the brand green #2E6B30 with Material's tonal palettes (the Theme Builder's mapping:
// primary chroma 48, secondary 16, tertiary hue +60 at 24, neutrals 4 and 8), primary pinned to the
// brand colour itself. Every role is set, surface containers included, so nothing falls back to the
// library's purple baseline.
private val appLightColors =
    lightColorScheme(
        primary = Color(0xFF2E6B30),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFAAF5A4),
        onPrimaryContainer = Color(0xFF002204),
        inversePrimary = Color(0xFF8FD88A),
        secondary = Color(0xFF52634F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD5E8CF),
        onSecondaryContainer = Color(0xFF101F0F),
        tertiary = Color(0xFF38656A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFBCEBF1),
        onTertiaryContainer = Color(0xFF002023),
        background = Color(0xFFF9FAF4),
        onBackground = Color(0xFF1A1C19),
        surface = Color(0xFFF9FAF4),
        onSurface = Color(0xFF1A1C19),
        surfaceVariant = Color(0xFFDEE5D8),
        onSurfaceVariant = Color(0xFF424940),
        surfaceTint = Color(0xFF2E6B30),
        inverseSurface = Color(0xFF2F312D),
        inverseOnSurface = Color(0xFFF0F1EB),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF72796F),
        outlineVariant = Color(0xFFC2C9BD),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFF9FAF4),
        surfaceContainer = Color(0xFFEEEEE8),
        surfaceContainerHigh = Color(0xFFE8E9E3),
        surfaceContainerHighest = Color(0xFFE2E3DD),
        surfaceContainerLow = Color(0xFFF3F4EE),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFDADAD5),
    )

private val appDarkColors =
    darkColorScheme(
        primary = Color(0xFF8FD88A),
        onPrimary = Color(0xFF00390B),
        primaryContainer = Color(0xFF055315),
        onPrimaryContainer = Color(0xFFAAF5A4),
        inversePrimary = Color(0xFF2E6B30),
        secondary = Color(0xFFBACCB3),
        onSecondary = Color(0xFF253423),
        secondaryContainer = Color(0xFF3B4B38),
        onSecondaryContainer = Color(0xFFD5E8CF),
        tertiary = Color(0xFFA0CFD4),
        onTertiary = Color(0xFF00363B),
        tertiaryContainer = Color(0xFF1F4D52),
        onTertiaryContainer = Color(0xFFBCEBF1),
        background = Color(0xFF121411),
        onBackground = Color(0xFFE2E3DD),
        surface = Color(0xFF121411),
        onSurface = Color(0xFFE2E3DD),
        surfaceVariant = Color(0xFF424940),
        onSurfaceVariant = Color(0xFFC2C9BD),
        surfaceTint = Color(0xFF8FD88A),
        inverseSurface = Color(0xFFE2E3DD),
        inverseOnSurface = Color(0xFF2F312D),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF8C9388),
        outlineVariant = Color(0xFF424940),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF383A36),
        surfaceContainer = Color(0xFF1E201D),
        surfaceContainerHigh = Color(0xFF282B27),
        surfaceContainerHighest = Color(0xFF333531),
        surfaceContainerLow = Color(0xFF1A1C19),
        surfaceContainerLowest = Color(0xFF0C0F0C),
        surfaceDim = Color(0xFF121411),
    )

/**
 * Record gold: the colours of every personal-record mark. Kept beside [MaterialTheme] rather than in
 * a scheme role, so records stay gold when dynamic colour is on.
 */
@Immutable
data class RecordColors(
    val record: Color,
    val onRecord: Color,
    val recordContainer: Color,
    val onRecordContainer: Color,
)

// Tones 40/100/90/10 (light) and 80/20/30/90 (dark) of a gold palette seeded from #C9A227.
private val lightRecordColors =
    RecordColors(
        record = Color(0xFF755B00),
        onRecord = Color(0xFFFFFFFF),
        recordContainer = Color(0xFFFFE08E),
        onRecordContainer = Color(0xFF241A00),
    )

private val darkRecordColors =
    RecordColors(
        record = Color(0xFFECC246),
        onRecord = Color(0xFF3D2E00),
        recordContainer = Color(0xFF584400),
        onRecordContainer = Color(0xFFFFE08E),
    )

private val LocalRecordColors = staticCompositionLocalOf { lightRecordColors }

/** The record gold of the current theme. */
val MaterialTheme.recordColors: RecordColors
    @Composable
    @ReadOnlyComposable
    get() = LocalRecordColors.current

/**
 * The app theme. Dynamic colour is off by default, so every phone shows the brand scheme; a Settings
 * toggle to turn it on comes later.
 */
@Composable
fun GymTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    CompositionLocalProvider(LocalRecordColors provides if (darkTheme) darkRecordColors else lightRecordColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

/**
 * This style with tabular (equal-width) figures, so numbers that change or sit in columns, such as
 * timers, set fields and totals, don't shift as their digits change.
 */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")
