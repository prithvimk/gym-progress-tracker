package io.github.prithvimk.gymtracker.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.timer.RestTimerState
import io.github.prithvimk.gymtracker.ui.format.formatClock
import io.github.prithvimk.gymtracker.ui.theme.tabular
import io.github.prithvimk.gymtracker.ui.workout.ENDING_MILLIS
import io.github.prithvimk.gymtracker.ui.workout.RestRing
import io.github.prithvimk.gymtracker.ui.workout.TICK_MS
import io.github.prithvimk.gymtracker.ui.workout.rememberElapsedMillis
import io.github.prithvimk.gymtracker.ui.workout.rememberRemainingMillis

/** The running workout as the mini bar shows it. [routineName] is null for an empty workout. */
data class MiniBarWorkout(val routineName: String?, val startedAt: Long)

/**
 * The bar above the navigation bar while a workout runs and another tab is showing: the routine, the
 * time since it started, and the rest countdown while one runs. Tapping it goes back to the workout.
 */
@Composable
fun WorkoutMiniBar(
    workout: MiniBarWorkout,
    rest: RestTimerState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = workout.routineName ?: stringResource(R.string.workout_active_title)
    val elapsed = formatClock(rememberElapsedMillis(workout.startedAt))
    val remainingMillis = rememberRemainingMillis(rest)
    // A rest whose time is up is shown no more, though the timer clears it a moment later.
    val restLeft = rest?.takeIf { remainingMillis > 0L }
    // Rounded up, as on the logger's chip, so it reads 0:01 until it is really over.
    val remaining = formatClock(remainingMillis + TICK_MS - 1L)
    val description =
        if (restLeft != null) {
            stringResource(R.string.mini_bar_description_resting, title, elapsed, remaining)
        } else {
            stringResource(R.string.mini_bar_description, title, elapsed)
        }
    val resumeLabel = stringResource(R.string.mini_bar_resume)
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(label = resumeLabel) {
                    onClick()
                    true
                }
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(min = 56.dp).padding(start = 12.dp, end = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tab_workout),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = elapsed,
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = SECONDARY_TEXT_ALPHA),
                    maxLines = 1,
                )
            }
            if (restLeft != null) {
                RestPill(
                    remaining = remaining,
                    fraction = (remainingMillis.toFloat() / restLeft.durationMillis.coerceAtLeast(1L)).coerceIn(0f, 1f),
                    ending = remainingMillis <= ENDING_MILLIS,
                )
            }
        }
    }
}

/** The rest countdown with its emptying ring, turning primary for the last few seconds as on the logger. */
@Composable
private fun RestPill(remaining: String, fraction: Float, ending: Boolean) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (ending) colors.primary else colors.surface, label = "rest")
    val content by animateColorAsState(if (ending) colors.onPrimary else colors.onSurface, label = "rest")
    CompositionLocalProvider(LocalContentColor provides content) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .background(container, CircleShape)
                .heightIn(min = 32.dp)
                .padding(start = 8.dp, end = 12.dp),
        ) {
            RestRing(fraction, content)
            Text(text = remaining, style = MaterialTheme.typography.labelLarge.tabular(), maxLines = 1)
        }
    }
}

/**
 * Screens that need the bottom of the screen to themselves, such as the keypad, ask for the mini bar to
 * give way with [HideMiniBar]. Each asks while it is in the composition.
 */
@Stable
class MiniBarHiders {
    var count by mutableIntStateOf(0)
        private set

    internal fun add() {
        count++
    }

    internal fun remove() {
        count--
    }
}

val LocalMiniBarHiders = staticCompositionLocalOf<MiniBarHiders?> { null }

/** Keeps the mini bar out of the way for as long as this is in the composition. */
@Composable
fun HideMiniBar() {
    val hiders = LocalMiniBarHiders.current ?: return
    DisposableEffect(hiders) {
        hiders.add()
        onDispose { hiders.remove() }
    }
}

private const val SECONDARY_TEXT_ALPHA = 0.8f
