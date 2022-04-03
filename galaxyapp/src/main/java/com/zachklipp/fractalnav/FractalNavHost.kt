package com.zachklipp.fractalnav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.zachklipp.galaxyapp.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A container that can host special composables defined with [FractalNavScope.FractalNavChild] that
 * can be [zoomed][FractalNavScope.zoomToChild] into and replace the content of this host.
 */
@Composable
fun FractalNavHost(
    modifier: Modifier = Modifier,
    zoomAnimationSpec: AnimationSpec<Float> = tween(2_000),
    content: @Composable FractalNavScope.() -> Unit
) {
    val updatedAnimationSpec by rememberUpdatedState(zoomAnimationSpec)
    val state = rememberFractalNavState(zoomAnimationSpec = { updatedAnimationSpec })
    FractalNavHost(state, modifier) {
        content(state)
    }
}

@Composable
private fun rememberFractalNavState(
    zoomAnimationSpec: () -> AnimationSpec<Float>
): FractalNavState {
    val coroutineScope = rememberCoroutineScope()
    val state = rememberSaveable(saver = FractalNavState.Saver) {
        FractalNavState(coroutineScope, zoomAnimationSpec)
    }
    return state
}

@Composable
private fun FractalNavHost(
    state: FractalNavState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val contentStateHolder = rememberSaveableStateHolder()
    val rootModifier = modifier
        // TODO why isn't onPlaced working??
        .onPlaced { state.viewportCoordinates = it }
        .onGloballyPositioned { state.viewportCoordinates = it }

    if (!state.isFullyZoomedIn) {
        contentStateHolder.SaveableStateProvider("fractal-nav-host-$currentCompositeKeyHash") {
            Box(
                modifier = rootModifier
                    .then(state.contentZoomModifier)
                    .onPlaced { state.scaledContentCoordinates = it }
                    .onGloballyPositioned { state.scaledContentCoordinates = it },
                propagateMinConstraints = true
            ) {
                content()
            }
        }
    }

    state.activeChild?.MovableContent(
        modifier = if (state.isFullyZoomedIn) rootModifier else state.childZoomModifier
    )
}

interface FractalNavScope {
    /**
     * Defines a child composable that can be zoomed into by calling [zoomToChild].
     * The [content] of this function is automatically placed inside its own [FractalNavHost], and
     * so can define its own children.
     */
    @Composable
    fun FractalNavChild(
        key: String,
        modifier: Modifier,
        content: @Composable FractalNavChildScope.() -> Unit
    )

    /**
     * Requests that the [FractalNavChild] passed the given [key] be activated and eventually
     * entirely replace the content of this [FractalNavHost].
     */
    fun zoomToChild(key: String)
}

enum class ZoomDirection {
    ZoomingIn,
    ZoomingOut
}

/**
 * A [FractalNavScope] that is also a child of a [FractalNavChild].
 */
interface FractalNavChildScope : FractalNavScope {
    /**
     * True if this [FractalNavChild] is being zoomed in or out of, or is fully zoomed in.
     * Only one sibling may be active at a time.
     * When this is false, [isFullyZoomedIn] will always be false.
     */
    val isActive: Boolean

    /**
     * True if this [FractalNavChild] is completely replacing its parent content.
     * When this is true, [isActive] will always be true.
     */
    val isFullyZoomedIn: Boolean

    /**
     * The amount that this child is zoomed, between 0 ([isActive]=false) and
     * 1 ([isFullyZoomedIn]=true).
     */
    val zoomFactor: Float

    /** Null when not zooming. */
    val zoomDirection: ZoomDirection?

    /**
     * True when a child of this scope is being zoomed in or has fully-zoomed in.
     * Always false initially.
     */
    val hasActiveChild: Boolean

    /**
     * When [hasActiveChild] is true, this is the zoom factor of the active child.
     */
    val childZoomFactor: Float

    /** Requests the parent of this [FractalNavChild] zoom it out and eventually deactivate it. */
    fun zoomToParent()

    fun Modifier.scaleByZoomFactor(): Modifier = scaleLayout { zoomFactor }
    fun Modifier.alphaByZoomFactor(): Modifier = graphicsLayer { alpha = zoomFactor }
}

private interface FractalParent {
    val zoomFactor: Float
    val activeChild: FractalChild?
    val zoomDirection: ZoomDirection?
    val zoomAnimationSpec: () -> AnimationSpec<Float>

    fun zoomOut()
}

private class FractalNavState(
    private val coroutineScope: CoroutineScope,
    override val zoomAnimationSpec: () -> AnimationSpec<Float>
) : FractalNavScope,
    FractalParent {
    private val children = mutableStateMapOf<String, FractalChild>()
    private val zoomFactorAnimatable = Animatable(0f)
    var viewportCoordinates: LayoutCoordinates? = null
    var scaledContentCoordinates: LayoutCoordinates? = null
    override val zoomFactor: Float by zoomFactorAnimatable.asState()
    val isFullyZoomedIn by derivedStateOf { zoomFactor == 1f }
    override var zoomDirection: ZoomDirection? by mutableStateOf(null)
        private set

    override var activeChild: FractalChild? by mutableStateOf(null)
    val placeholderBounds: Rect?
        get() {
            val coords = scaledContentCoordinates?.takeIf { it.isAttached } ?: return null
            val childCoords = activeChild?.coordinates?.takeIf { it.isAttached } ?: return null
            return coords.localBoundingBoxOf(childCoords, clipBounds = false)
        }
    private var childPlaceholderSize: IntSize? by mutableStateOf(null)

    /**
     * A pair of values representing the x and y scale factors that the content needs to be scaled
     * by to make the placeholder fill the screen.
     */
    private val activeChildScaleTarget: Offset
        get() {
            val coords = viewportCoordinates?.takeIf { it.isAttached } ?: return Offset(1f, 1f)
            val childSize = childPlaceholderSize ?: return Offset(1f, 1f)
            return Offset(
                x = coords.size.width / childSize.width.toFloat(),
                y = coords.size.height / childSize.height.toFloat()
            )
        }

    val contentZoomModifier
        get() = if (activeChild != null && !isFullyZoomedIn) {
            Modifier
                // The blur should not scale with the rest of the content, so it needs to be put
                // in a separate layer.
                .graphicsLayer {
                    // Radius of 0f causes a crash.
                    val blurRadius = 0.000001f + (zoomFactor * 40.dp.toPx())
                    renderEffect = BlurEffect(blurRadius, blurRadius)
                    // Fade the content out so that if it's drawing its own background it won't blink
                    // in and out when the content enters/leaves the composition.
                    alpha = 1f - zoomFactor
                    clip = true
                }
                .graphicsLayer {
                    val scaleTarget = activeChildScaleTarget
                    scaleX = lerp(1f, scaleTarget.x, zoomFactor)
                    scaleY = lerp(1f, scaleTarget.y, zoomFactor)

                    // The scale needs to happen around the center of the placeholder.
                    val coords =
                        scaledContentCoordinates?.takeIf { it.isAttached } ?: return@graphicsLayer
                    val childBounds = placeholderBounds ?: return@graphicsLayer
                    val pivot = TransformOrigin(
                        pivotFractionX = childBounds.center.x / (coords.size.width),
                        pivotFractionY = childBounds.center.y / (coords.size.height)
                    )
                    transformOrigin = pivot

                    // And we need to translate so that the left edge of the placeholder will be
                    // eventually moved to the left edge of the viewport.
                    val parentCenter = viewportCoordinates!!.size.center.toOffset()
                    val distanceToCenter = parentCenter - childBounds.center
                    translationX = zoomFactor * distanceToCenter.x
                    translationY = zoomFactor * distanceToCenter.y
                }
        } else Modifier

    /**
     * Modifier that is applied to the active child when it's currently being zoomed and has been
     * removed from the content composition to be composed by the host instead.
     */
    val childZoomModifier = Modifier
        .layout { measurable, constraints ->
            // But scale the layout bounds up instead, so the child will grow to fill the space
            // previously filled by the content.
            val childSize = childPlaceholderSize!!.toSize()
            val parentSize = viewportCoordinates!!.size.toSize()
            val scaledSize = lerp(childSize, parentSize, zoomFactor)
            val scaledConstraints = Constraints(
                minWidth = scaledSize.width.roundToInt(),
                minHeight = scaledSize.height.roundToInt(),
                maxWidth = scaledSize.width.roundToInt(),
                maxHeight = scaledSize.height.roundToInt()
            )
            val placeable = measurable.measure(scaledConstraints)
            layout(
                constraints.constrainWidth(placeable.width),
                constraints.constrainHeight(placeable.height)
            ) {
                val coords = viewportCoordinates!!
                val childCoords = activeChild?.coordinates?.takeIf { it.isAttached }
                // If the activeChild is null, that means the animation finished _just_ before this
                // placement pass – e.g. the user could have been scrolling the content while the
                // animation was still running.
                if (childCoords == null) {
                    placeable.place(IntOffset.Zero)
                } else {
                    val placeholderBounds =
                        coords.localBoundingBoxOf(childCoords, clipBounds = false)
                    placeable.place(
                        placeholderBounds.topLeft.round()
                    )
                }
            }
        }

    @Composable
    override fun FractalNavChild(
        key: String,
        modifier: Modifier,
        content: @Composable FractalNavChildScope.() -> Unit
    ) {
        check(!isFullyZoomedIn) {
            "FractalNavHost content shouldn't be composed when fully zoomed in."
        }

        val child = children.getOrPut(key) { remember { FractalChild(parent = this) } }
        // When this function is removed from composition because it's the active child and fully
        // zoomed in, we want to keep its state around so we keep the same object when zooming out
        // again. Otherwise, we don't care about this child's state so we can clean it out of the
        // map.
        DisposableEffect(child, key) {
            onDispose {
                if (!(isFullyZoomedIn && activeChild === child)) {
                    children -= key
                }
            }
        }

        child.setContent(content)

        val childModifier = modifier
            // TODO why isn't onPlaced working?
            .onPlaced { child.coordinates = it }
            .onGloballyPositioned { child.coordinates = it }

        // The active child will be composed by the host, so don't compose it here.
        if (activeChild === child) {
            // …but put a placeholder instead to reserve the space.
            childPlaceholderSize?.run {
                Box(childModifier.size(
                    with(LocalDensity.current) {
                        DpSize(width.toDp(), height.toDp())
                    }
                ))
            }
        } else {
            child.MovableContent(childModifier)
        }
    }

    override fun zoomToChild(key: String) {
        val requestedChild = children.getOrElse(key) {
            throw IllegalArgumentException("No child with key \"$key\".")
        }

        // Can't zoom into a child while a different child is currently active.
        // However, if we're currently zooming out of a child, and that same child requested to
        // be zoomed in again, we can cancel the zoom out and start zooming in immediately.
        if (activeChild != null &&
            (activeChild !== requestedChild || zoomDirection != ZoomDirection.ZoomingOut)
        ) {
            return
        }
        activeChild = requestedChild

        // Capture the size before starting the animation since the child's layout node will be
        // removed from wherever it is but we need the placeholder to preserve that space.
        // This could probably be removed once speculative layout exists.
        childPlaceholderSize = activeChild?.coordinates?.takeIf { it.isAttached }?.size
        zoomDirection = ZoomDirection.ZoomingIn

        coroutineScope.launch {
            zoomFactorAnimatable.animateTo(1f, zoomAnimationSpec())
            zoomDirection = null
        }
    }

    override fun zoomOut() {
        check(activeChild != null) { "Already zoomed out." }
        zoomDirection = ZoomDirection.ZoomingOut
        coroutineScope.launch {
            zoomFactorAnimatable.animateTo(0f, zoomAnimationSpec())
            activeChild = null
            zoomDirection = null
        }
    }

    object Saver : androidx.compose.runtime.saveable.Saver<FractalNavState, Any> {
        override fun SaverScope.save(value: FractalNavState): Any? {
            return null
        }

        override fun restore(value: Any): FractalNavState? {
            return null
        }
    }
}

private class FractalChild(private val parent: FractalParent) {
    private var _content: (@Composable FractalNavChildScope.() -> Unit)? by mutableStateOf(null)
    private var refCount = 0
    var coordinates: LayoutCoordinates? = null

    /**
     * A [movableContentOf] wrapper that allows all state inside the child's composable to be moved
     * from being composed from inside the content of a [FractalNavHost] to being the root of
     * the host.
     */
    private val movableContent = movableContentOf { modifier: Modifier ->
        val childState = rememberFractalNavState(parent.zoomAnimationSpec)
        val childScope = remember(childState) { ChildScope(childState) }
        FractalNavHost(childState, modifier) {
            _content?.invoke(childScope)

            DisposableEffect(this) {
                check(refCount == 0) {
                    "Movable content was composed more than once! refCount=$refCount"
                }
                refCount++
                onDispose {
                    refCount--
                }
            }
        }
    }

    fun setContent(content: @Composable FractalNavChildScope.() -> Unit) {
        _content = content
    }

    @Suppress("NOTHING_TO_INLINE")
    @Composable
    inline fun MovableContent(modifier: Modifier = Modifier) {
        movableContent(modifier)
    }

    private inner class ChildScope(private val state: FractalNavState) : FractalNavScope by state,
        FractalNavChildScope {
        override val isActive: Boolean by derivedStateOf { parent.activeChild === this@FractalChild }
        override val isFullyZoomedIn: Boolean by derivedStateOf { isActive && parent.zoomFactor == 1f }
        override val zoomFactor: Float by derivedStateOf { if (isActive) parent.zoomFactor else 0f }
        override val zoomDirection: ZoomDirection?
            get() = if (isActive) parent.zoomDirection else null
        override val hasActiveChild: Boolean
            get() = state.activeChild !== null
        override val childZoomFactor: Float
            get() = state.zoomFactor

        override fun zoomToParent() {
            parent.zoomOut()
        }
    }
}