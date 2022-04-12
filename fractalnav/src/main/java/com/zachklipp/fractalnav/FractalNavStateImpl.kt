package com.zachklipp.fractalnav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import com.zachklipp.fractalnav.ZoomDirection.ZoomingOut
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class FractalNavStateImpl : FractalNavState, FractalNavScope, FractalParent {
    // These do not need to be backed by snapshot state because they're set in a side effect.
    lateinit var coroutineScope: CoroutineScope
    override lateinit var zoomAnimationSpecFactory: () -> AnimationSpec<Float>

    private val children = mutableMapOf<String, FractalChild>()
    private val zoomFactorAnimatable = Animatable(0f)
    private val zoomFactor: Float by zoomFactorAnimatable.asState()

    val isFullyZoomedIn by derivedStateOf { zoomFactor == 1f }
    override var zoomDirection: ZoomDirection? by mutableStateOf(null)
        private set

    /**
     * When true, the nav hosts's content should be composed. This is false when a child is fully
     * zoomed-in AND is not imminently starting to zoom out. We compose when starting to zoom out
     * even before the [zoomFactor] animation has started to give the content a chance to initialize
     * before starting the animation, to avoid jank caused by an extra long first-frame.
     */
    val composeContent: Boolean get() = !isFullyZoomedIn || zoomDirection == ZoomingOut

    var viewportCoordinates: LayoutCoordinates? = null
    var scaledContentCoordinates: LayoutCoordinates? by mutableStateOf(null)

    override val viewportWidth: Int
        get() = viewportCoordinates?.size?.width ?: 0

    override val hasActiveChild: Boolean get() = activeChild != null
    override val childZoomFactor: Float get() = zoomFactor

    /**
     * The child that is currently either zoomed in or out, or fully zoomed in. Null when fully
     * zoomed out.
     */
    override var activeChild: FractalChild? by mutableStateOf(null)

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun FractalNavChild(
        key: String,
        modifier: Modifier,
        content: @Composable FractalNavChildScope.() -> Unit
    ) {

        val child = remember {
            if (activeChild?.key == key) {
                // If there's an active child on the first composition, that means that we're
                // starting a zoom-out and should move the child into the content composition
                // here by re-using the child's state.
                activeChild!!
            } else {
                FractalChild(key, parent = this)
            }
        }

        // Register the child with its key so that zoomToChild can find it.
        DisposableEffect(child) {
            check(key !in children) {
                "FractalNavChild with key \"$key\" has already been composed."
            }
            children[key] = child
            onDispose {
                // TODO uncomment this once I figure out why the state is being re-initialized.
                //child.placeholderCoordinates = null
                children -= key
            }
        }

        child.setContent(content)

        Box(
            modifier = modifier
                .onPlaced { child.placeholderCoordinates = it }
                .workaroundBoxOnPlacedBug()
                .bringIntoViewRequester(child.bringIntoViewRequester),
            propagateMinConstraints = true
        ) {
            // The active child is always composed directly by the host, so when that happens
            // remove it from the composition here so the movable content is moved and not
            // re-instantiated.
            if (child !== activeChild) {
                child.MovableContent()
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun zoomToChild(key: String) {
        // Can't zoom into a child while a different child is currently active.
        // However, if we're currently zooming out of a child, and that same child requested to
        // be zoomed in again, we can cancel the zoom out and start zooming in immediately.
        if (activeChild != null &&
            (activeChild?.key != key || zoomDirection != ZoomingOut)
        ) {
            return
        }

        val requestedChild = children.getOrElse(key) {
            throw IllegalArgumentException("No child with key \"$key\".")
        }
        activeChild = requestedChild
        zoomDirection = ZoomDirection.ZoomingIn

        coroutineScope.launch {
            try {
                // Try to make the child fully visible before starting zoom so the scale animation
                // doesn't end up scaling a clipped view.
                zoomFactorAnimatable.animateTo(1f, zoomAnimationSpecFactory())
            } finally {
                // If we weren't cancelled by an opposing animation, jump to the end state.
                if (zoomDirection == ZoomDirection.ZoomingIn) {
                    zoomDirection = null
                }
            }
        }
    }

    override fun zoomOut() {
        // If already zooming or zoomed out, do nothing.
        if (activeChild == null || zoomDirection == ZoomingOut) return

        zoomDirection = ZoomingOut
        coroutineScope.launch {
            // Composing the parent for the first time can take some time. Wait a frame before
            // animating to give it a chance to settle.
            withFrameMillis {}
            try {
                zoomFactorAnimatable.animateTo(0f, zoomAnimationSpecFactory())
            } finally {
                // If we weren't cancelled by an opposing animation, jump to the end state.
                if (zoomDirection == ZoomingOut) {
                    activeChild = null
                    zoomDirection = null
                }
            }
        }
    }
}