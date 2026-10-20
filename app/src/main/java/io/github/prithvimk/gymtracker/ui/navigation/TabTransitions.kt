package io.github.prithvimk.gymtracker.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn

/*
 * Material "fade through" for switching between bottom navigation tabs: the old tab fades out quickly,
 * then the new tab fades in while scaling up from 92%. The two never overlap.
 */
private const val FADE_OUT_MILLIS = 90
private const val FADE_IN_MILLIS = 210
private const val INITIAL_SCALE = 0.92f

val tabEnterTransition: EnterTransition =
    fadeIn(tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS, easing = LinearOutSlowInEasing)) +
        scaleIn(
            animationSpec = tween(FADE_IN_MILLIS, delayMillis = FADE_OUT_MILLIS, easing = LinearOutSlowInEasing),
            initialScale = INITIAL_SCALE,
        )

val tabExitTransition: ExitTransition = fadeOut(tween(FADE_OUT_MILLIS, easing = FastOutLinearInEasing))
