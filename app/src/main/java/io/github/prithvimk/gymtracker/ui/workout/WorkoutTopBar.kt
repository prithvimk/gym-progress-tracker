package io.github.prithvimk.gymtracker.ui.workout

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.prithvimk.gymtracker.R
import io.github.prithvimk.gymtracker.timer.RestTimer
import io.github.prithvimk.gymtracker.timer.RestTimerState
import io.github.prithvimk.gymtracker.ui.format.formatClock
import io.github.prithvimk.gymtracker.ui.theme.tabular
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Rest timer controls. [onAdjust] takes seconds to add, negative to take off. */
@Immutable
class RestActions(val onAdjust: (seconds: Int) -> Unit, val onSkip: () -> Unit) {
    companion object {
        val None = RestActions({}, {})
    }
}

/**
 * The logger's top bar: the routine's name, a time chip, Finish, and a menu with the rarer actions.
 * Discard sits in the menu, two deliberate taps away from Finish.
 *
 * The chip shows the time since [startedAt], or the countdown while [rest] runs. During rest, tapping
 * the title peeks at the elapsed time: the chip spins round to it for [PEEK_MILLIS], and tapping the
 * title again spins it back sooner. [showPeekHint] points this out on the first rest, and
 * [onPeekHintShown] is called once it has been shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutTopBar(
    title: String,
    startedAt: Long,
    rest: RestTimerState?,
    restActions: RestActions,
    onFinishClick: () -> Unit,
    onNotesClick: () -> Unit,
    onAddExercisesClick: () -> Unit,
    onDiscardClick: () -> Unit,
    showPeekHint: Boolean = false,
    onPeekHintShown: () -> Unit = {},
) {
    val resting = rest != null
    val scope = rememberCoroutineScope()
    // 1 when a peek starts, draining to 0 over PEEK_MILLIS; the chip shows the elapsed time while above 0.
    val peek = remember { Animatable(0f) }
    val peeking by remember { derivedStateOf { peek.value > 0f } }
    // A rest that ends mid-peek leaves the chip on the elapsed time, where it stays.
    LaunchedEffect(resting) { if (!resting) peek.snapTo(0f) }
    val hint = rememberTooltipState(isPersistent = true)
    LaunchedEffect(resting, showPeekHint) {
        if (resting && showPeekHint) {
            onPeekHintShown()
            hint.show()
        }
    }
    val togglePeek: () -> Unit = {
        hint.dismiss()
        scope.launch {
            if (peek.value > 0f) {
                peek.snapTo(0f)
            } else {
                peek.snapTo(1f)
                peek.animateTo(0f, tween(PEEK_MILLIS, easing = LinearEasing))
            }
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            TooltipBox(
                positionProvider = rememberBelowAnchorPositionProvider(centered = false, gap = HINT_GAP),
                tooltip = { PlainTooltip { Text(stringResource(R.string.workout_peek_hint)) } },
                state = hint,
                enableUserInput = false,
            ) {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            enabled = resting,
                            onClickLabel = stringResource(R.string.workout_peek_action),
                            onClick = togglePeek,
                        ).padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
        },
        actions = {
            // The chip and Finish are one size: both take the width of the wider of the two.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.width(IntrinsicSize.Max),
            ) {
                TimeChip(
                    startedAt = startedAt,
                    rest = rest,
                    peeking = peeking,
                    peekLeft = { peek.value },
                    onPeek = togglePeek,
                    restActions = restActions,
                    modifier = Modifier.weight(1f).height(TOP_BAR_PILL_HEIGHT),
                )
                Button(
                    onClick = onFinishClick,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.weight(1f).height(TOP_BAR_PILL_HEIGHT),
                ) {
                    Text(stringResource(R.string.workout_finish))
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.workout_more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.workout_notes)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_notes), contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onNotesClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.workout_add_exercises)) },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onAddExercisesClick()
                        },
                    )
                    val error = MaterialTheme.colorScheme.error
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.workout_discard)) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        colors = MenuDefaults.itemColors(textColor = error, leadingIconColor = error),
                        onClick = {
                            menuOpen = false
                            onDiscardClick()
                        },
                    )
                }
            }
        },
    )
}

/**
 * The top bar's time chip. Outside rest it shows the elapsed time. During [rest] it shows the countdown
 * with a ring that empties, turns primary and pulses in the last [ENDING_MILLIS], and opens −15 / +15 /
 * Skip when tapped. While [peeking] it spins round to the elapsed time in inverted colours, with a line
 * that drains as [peekLeft] goes from 1 to 0. With animations off, the faces cross-fade instead.
 */
@Composable
private fun TimeChip(
    startedAt: Long,
    rest: RestTimerState?,
    peeking: Boolean,
    peekLeft: () -> Float,
    onPeek: () -> Unit,
    restActions: RestActions,
    modifier: Modifier = Modifier,
) {
    val elapsed = formatClock(rememberElapsedMillis(startedAt))
    val remainingMillis = rememberRemainingMillis(rest)
    // Rounded up, so it reads 0:01 until it is really over.
    val remaining = formatClock(remainingMillis + TICK_MS - 1L)
    val resting = rest != null
    val showElapsed = !resting || peeking
    val ending = resting && remainingMillis <= ENDING_MILLIS
    val animationsOff = rememberAnimationsOff()
    val colors = MaterialTheme.colorScheme

    // The rest face, primary for the last few seconds.
    val restContainer by animateColorAsState(if (ending) colors.primary else colors.secondaryContainer, label = "rest")
    val restContent by animateColorAsState(
        if (ending) colors.onPrimary else colors.onSecondaryContainer,
        label = "rest"
    )
    // The elapsed face: inverted while peeking during rest, so it is clear it will flip back.
    val elapsedContainer by animateColorAsState(
        if (resting) colors.inverseSurface else colors.surfaceContainerHigh,
        label = "elapsed",
    )
    val elapsedContent by animateColorAsState(
        if (resting) colors.inverseOnSurface else colors.onSurface,
        label = "elapsed",
    )
    // 0 shows the rest face, 180 the elapsed face.
    val turn by animateFloatAsState(
        targetValue = if (showElapsed) 180f else 0f,
        animationSpec =
        if (animationsOff) {
            snap()
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "turn",
    )
    val fade by animateFloatAsState(if (showElapsed) 1f else 0f, tween(FADE_MILLIS), label = "fade")
    val pulse = remember { Animatable(1f) }
    val pulsing = ending && !showElapsed && !animationsOff
    LaunchedEffect(pulsing) {
        if (pulsing) {
            pulse.animateTo(PULSE_SCALE, infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse))
        } else {
            pulse.animateTo(1f, tween(PULSE_MILLIS))
        }
    }

    var controlsOpen by remember { mutableStateOf(false) }
    LaunchedEffect(peeking) { if (peeking) controlsOpen = false }
    val openControls = {
        if (peeking) onPeek()
        controlsOpen = true
    }
    val description =
        if (resting) {
            stringResource(R.string.workout_rest_chip_description, remaining, elapsed)
        } else {
            stringResource(R.string.workout_elapsed_description, elapsed)
        }
    val adjustLabel = stringResource(R.string.workout_rest_adjust)
    val peekLabel = stringResource(R.string.workout_peek_action)
    val density = LocalDensity.current.density

    Box(modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pulse.value
                    scaleY = pulse.value
                    rotationY = turn
                    cameraDistance = CAMERA_DISTANCE * density
                }.clip(CircleShape)
                .clickable(enabled = resting, onClick = openControls)
                .clearAndSetSemantics {
                    contentDescription = description
                    if (resting) {
                        role = Role.Button
                        onClick(label = adjustLabel) {
                            openControls()
                            true
                        }
                        customActions = listOf(
                            CustomAccessibilityAction(peekLabel) {
                                onPeek()
                                true
                            },
                        )
                    }
                },
        ) {
            ChipFace(
                container = restContainer,
                content = restContent,
                modifier = Modifier.graphicsLayer {
                    alpha = if (animationsOff) {
                        1f - fade
                    } else if (turn < 90f) {
                        1f
                    } else {
                        0f
                    }
                },
            ) {
                val fraction = if (rest != null && rest.durationMillis > 0L) {
                    (remainingMillis.toFloat() / rest.durationMillis).coerceIn(0f, 1f)
                } else {
                    0f
                }
                RestRing(fraction, restContent)
                ChipText(remaining)
            }
            ChipFace(
                container = elapsedContainer,
                content = elapsedContent,
                modifier = Modifier
                    .graphicsLayer {
                        // Turned the other way, so it reads normally once the chip has turned round.
                        rotationY = if (animationsOff) 0f else 180f
                        alpha = if (animationsOff) {
                            fade
                        } else if (turn >= 90f) {
                            1f
                        } else {
                            0f
                        }
                    }.drawWithContent {
                        drawContent()
                        // The peek's line, over the face, draining from the right as the peek runs out.
                        val left = peekLeft()
                        if (left <= 0f) return@drawWithContent
                        val inset = PEEK_LINE_INSET.toPx()
                        val y = size.height - PEEK_LINE_BOTTOM.toPx()
                        val end = inset + (size.width - 2 * inset) * left
                        drawLine(
                            color = elapsedContent.copy(alpha = PEEK_LINE_ALPHA),
                            start = Offset(inset, y),
                            end = Offset(end, y),
                            strokeWidth = PEEK_LINE_WIDTH.toPx(),
                            cap = StrokeCap.Round,
                        )
                    },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_rest_timer),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                ChipText(elapsed)
            }
        }
        RestControls(
            expanded = controlsOpen && resting,
            actions = restActions,
            onDismiss = { controlsOpen = false },
        )
    }
}

/** One side of the time chip: a pill of [container] with [content] centred in it. */
@Composable
private fun ChipFace(container: Color, content: Color, modifier: Modifier = Modifier, body: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides content) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxSize()
                .background(container, CircleShape)
                .padding(start = 12.dp, end = 16.dp),
        ) {
            body()
        }
    }
}

@Composable
private fun ChipText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.tabular(),
        maxLines = 1,
        modifier = Modifier.padding(start = 4.dp),
    )
}

/** A small ring that empties as the rest runs out; [fraction] is the share of it left. */
@Composable
private fun RestRing(fraction: Float, color: Color) {
    Canvas(modifier = Modifier.size(18.dp).padding(1.dp)) {
        val stroke = RING_STROKE.toPx()
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawArc(color.copy(alpha = RING_TRACK_ALPHA), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(color, -90f, 360f * fraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

/**
 * −15 / +15 / Skip in a pill centred under the rest chip, opened by tapping it. It stays open for more
 * adjustments until Skip, a tap outside it, or Back.
 */
@Composable
private fun RestControls(expanded: Boolean, actions: RestActions, onDismiss: () -> Unit) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = expanded
    // Kept until the closing animation has finished.
    if (!visible.currentState && !visible.targetState) return
    Popup(
        popupPositionProvider = rememberBelowAnchorPositionProvider(centered = true, gap = REST_CONTROLS_GAP),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        val growFromChip = TransformOrigin(0.5f, 0f)
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(MENU_ANIMATION_MILLIS)) +
                scaleIn(
                    tween(MENU_ANIMATION_MILLIS),
                    initialScale = MENU_INITIAL_SCALE,
                    transformOrigin = growFromChip
                ),
            exit = fadeOut(tween(MENU_ANIMATION_MILLIS)) +
                scaleOut(
                    tween(MENU_ANIMATION_MILLIS),
                    targetScale = MENU_INITIAL_SCALE,
                    transformOrigin = growFromChip
                ),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                // Inset so the shadow isn't clipped by the popup's edges.
                modifier = Modifier.padding(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.rest_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    RestStepButton(
                        label = stringResource(R.string.rest_subtract),
                        description = stringResource(R.string.rest_subtract_description),
                        onClick = { actions.onAdjust(-RestTimer.ADJUST_STEP_SECONDS) },
                    )
                    RestStepButton(
                        label = stringResource(R.string.rest_add),
                        description = stringResource(R.string.rest_add_description),
                        onClick = { actions.onAdjust(RestTimer.ADJUST_STEP_SECONDS) },
                    )
                    TextButton(
                        onClick = {
                            onDismiss()
                            actions.onSkip()
                        },
                    ) {
                        Text(stringResource(R.string.rest_skip))
                    }
                }
            }
        }
    }
}

/** A tonal −15 / +15 button, read out by TalkBack as [description]. */
@Composable
private fun RestStepButton(label: String, description: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            onClick {
                onClick()
                true
            }
        },
    ) {
        Text(label)
    }
}

/** The time since [startedAt], ticking each second. Worked out from [startedAt], so it survives process death. */
@Composable
private fun rememberElapsedMillis(startedAt: Long): Long {
    val elapsed by produceState(System.currentTimeMillis() - startedAt, startedAt) {
        while (true) {
            value = System.currentTimeMillis() - startedAt
            delay(TICK_MS)
        }
    }
    return elapsed
}

/** The time left in [rest], ticking each second, or 0 without one. */
@Composable
private fun rememberRemainingMillis(rest: RestTimerState?): Long {
    val remaining by produceState(rest?.remainingMillis(System.currentTimeMillis()) ?: 0L, rest) {
        if (rest == null) {
            value = 0L
            return@produceState
        }
        value = rest.remainingMillis(System.currentTimeMillis())
        while (value > 0L) {
            // Tick on the second boundary of the time left, so the display never skips a second.
            delay(value % TICK_MS + 1L)
            value = rest.remainingMillis(System.currentTimeMillis())
        }
    }
    return remaining
}

/**
 * Puts a popup [gap] under its anchor, centred on it when [centered], else lined up with its start, and
 * kept inside the window. The default tooltip position centres the hint over the title, which pushes it
 * off the left edge or under the status bar.
 */
@Composable
private fun rememberBelowAnchorPositionProvider(centered: Boolean, gap: Dp): PopupPositionProvider {
    val gapPx = with(LocalDensity.current) { gap.roundToPx() }
    return remember(centered, gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
                val start = when {
                    centered -> anchorBounds.center.x - popupContentSize.width / 2
                    layoutDirection == LayoutDirection.Ltr -> anchorBounds.left
                    else -> anchorBounds.right - popupContentSize.width
                }
                return IntOffset(start.coerceIn(0, maxX), anchorBounds.bottom + gapPx)
            }
        }
    }
}

/** Whether the system's animator duration scale is 0, as when "Remove animations" is on. */
@Composable
private fun rememberAnimationsOff(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** How long a peek at the elapsed time lasts before the chip spins back to the rest. */
internal const val PEEK_MILLIS = 5_000

private const val TICK_MS = 1_000L
private const val ENDING_MILLIS = 10_000L
private const val FADE_MILLIS = 200
private const val PULSE_MILLIS = 500
private const val PULSE_SCALE = 1.07f
private const val CAMERA_DISTANCE = 12f
private const val RING_TRACK_ALPHA = 0.25f
private const val PEEK_LINE_ALPHA = 0.55f
private val RING_STROKE = 2.5.dp
private val HINT_GAP = 4.dp

// The pill's own 4 dp inset makes this an 8 dp gap under the chip.
private val REST_CONTROLS_GAP = 4.dp
private const val MENU_ANIMATION_MILLIS = 150
private const val MENU_INITIAL_SCALE = 0.8f
private val PEEK_LINE_INSET = 12.dp
private val PEEK_LINE_BOTTOM = 5.dp
private val PEEK_LINE_WIDTH = 2.dp

// The height of a Material button, which the time chip matches.
private val TOP_BAR_PILL_HEIGHT = 40.dp
