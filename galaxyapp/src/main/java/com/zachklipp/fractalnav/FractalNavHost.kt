package com.zachklipp.fractalnav

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced

/**
 * A container that can host special composables defined with [FractalNavScope.FractalNavChild] that
 * can be [zoomed][FractalNavScope.zoomToChild] into and replace the content of this host.
 *
 * To preserve navigation state across configuration changes, create your own [FractalNavState] and
 * store it in a retained configuration instance (e.g. an AAC `ViewModel`).
 */
@NonRestartableComposable
@Composable
fun FractalNavHost(
    modifier: Modifier = Modifier,
    state: FractalNavState = remember { FractalNavState() },
    zoomAnimationSpec: AnimationSpec<Float> = FractalNavState.DefaultZoomAnimationSpec,
    content: @Composable FractalNavScope.() -> Unit
) {
    FractalNavHost(
        state = state,
        modifier = modifier,
        zoomAnimationSpecFactory = { zoomAnimationSpec },
        content = content
    )
}

/**
 * Stores the navigation state for a [FractalNavHost].
 * Should only be passed to a single [FractalNavHost] at a time.
 */
sealed interface FractalNavState {
    companion object {
        val DefaultZoomAnimationSpec: AnimationSpec<Float> = tween(2_000, easing = LinearEasing)
    }
}

fun FractalNavState(): FractalNavState = FractalNavStateImpl()

@Composable
internal fun FractalNavHost(
    state: FractalNavState,
    modifier: Modifier,
    zoomAnimationSpecFactory: () -> AnimationSpec<Float>,
    content: @Composable FractalNavScope.() -> Unit
) {
    state as FractalNavStateImpl
    val coroutineScope = rememberCoroutineScope()
    SideEffect {
        state.coroutineScope = coroutineScope
        state.zoomAnimationSpecFactory = zoomAnimationSpecFactory
    }

    val contentStateHolder = rememberSaveableStateHolder()
    val rootModifier = modifier
        // TODO why isn't onPlaced working??
        .onPlaced { state.viewportCoordinates = it }
        .onGloballyPositioned { state.viewportCoordinates = it }

    if (!state.isFullyZoomedIn) {
        contentStateHolder.SaveableStateProvider("fractal-nav-host") {
            Box(
                modifier = rootModifier
                    .then(state.contentZoomModifier)
                    .onPlaced { state.scaledContentCoordinates = it }
                    .onGloballyPositioned { state.scaledContentCoordinates = it },
                propagateMinConstraints = true
            ) {
                content(state)
            }
        }
    }

    state.activeChild?.MovableContent(
        modifier = if (state.isFullyZoomedIn) rootModifier else state.childZoomModifier
    )
}