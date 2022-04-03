package com.zachklipp.fractalnav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
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
    zoomAnimationSpec: AnimationSpec<Float> = tween(1_000),
    content: @Composable FractalNavScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val state = rememberSaveable(saver = FractalNavState.Saver) {
        FractalNavState(coroutineScope, zoomAnimationSpec)
    }
    SideEffect {
        state.zoomAnimationSpec = zoomAnimationSpec
    }
    val contentStateHolder = rememberSaveableStateHolder()
    val rootModifier = modifier
        // TODO why isn't onPlaced working??
        .onPlaced { state.coordinates = it }
        .onGloballyPositioned { state.coordinates = it }

    if (!state.isFullyZoomedIn) {
        contentStateHolder.SaveableStateProvider("fractal-nav-host-$currentCompositeKeyHash") {
            Box(
                modifier = rootModifier.then(state.contentZoomModifier),
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

    /** Requests the parent of this [FractalNavChild] zoom it out and eventually deactivate it. */
    fun zoomToParent()
}

private interface FractalParent {
    val zoomFactor: Float
    val activeChild: FractalChild?
    val zoomDirection: ZoomDirection?

    fun zoomOut()
}

private class FractalNavState(
    private val coroutineScope: CoroutineScope,
    var zoomAnimationSpec: AnimationSpec<Float>
) : FractalNavScope,
    FractalParent {
    private val children = mutableStateMapOf<String, FractalChild>()
    private val zoomFactorAnimatable = Animatable(0f)
    var coordinates: LayoutCoordinates? = null
    override val zoomFactor: Float by zoomFactorAnimatable.asState()
    val isFullyZoomedIn by derivedStateOf { zoomFactor == 1f }
    override var zoomDirection: ZoomDirection? = null
        private set

    override var activeChild: FractalChild? by mutableStateOf(null)
    var activeChildBounds: Rect? = null
        private set

    val activeChildPivot: TransformOrigin?
        get() {
            val coords = coordinates?.takeIf { it.isAttached } ?: return null
            val childBounds = activeChildBounds ?: return null
            return TransformOrigin(
                pivotFractionX = childBounds.center.x / coords.size.width,
                pivotFractionY = childBounds.center.y / coords.size.height
            )
        }

    val activeChildScaleTarget: Offset
        get() {
            val coords = coordinates?.takeIf { it.isAttached } ?: return Offset(1f, 1f)
            val childCoords = activeChild?.coordinates
                ?.takeIf { it.isAttached }
                ?: return Offset(1f, 1f)
            return Offset(
                x = coords.size.width / childCoords.size.width.toFloat(),
                y = coords.size.height / childCoords.size.height.toFloat()
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
                    scaleX = 1f + (scaleTarget.x * zoomFactor)
                    scaleY = 1f + (scaleTarget.y * zoomFactor)
                    activeChildPivot?.let { pivot ->
                        transformOrigin = pivot
                        val parentCenter = coordinates!!.size.center.toOffset()
                        val distanceToCenter = parentCenter - activeChildBounds!!.center
                        translationX = zoomFactor * distanceToCenter.x
                        translationY = zoomFactor * distanceToCenter.y
                    }
                }
        } else Modifier

    val childZoomModifier = Modifier
        .layout { measurable, constraints ->
            // But scale the layout bounds up instead, so the child will grow to fill the space
            // previously filled by the content.
            val childSize = activeChildBounds!!.size
            val parentSize = coordinates!!.size.toSize()
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
                val offset = lerp(activeChildBounds!!.topLeft, Offset.Zero, zoomFactor)
                placeable.place(
                    offset.round()
                )
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
        if (activeChild !== child) {
            child.MovableContent(childModifier)
        } else {
            // …but put a placeholder instead to reserve the space.
            Box(childModifier.size(activeChildBounds!!.size.run {
                with(LocalDensity.current) {
                    DpSize(width.toDp(), height.toDp())
                }
            }))
        }
    }

    override fun zoomToChild(key: String) {
        // Ignore requests when already zoomed.
        if (activeChild != null) return

        zoomDirection = ZoomDirection.ZoomingIn
        activeChild = children.getOrElse(key) {
            throw IllegalArgumentException("No child with key \"$key\".")
        }

        // Capture the bounds before starting the animation since the bounds will be animated.
        // Doing it this way makes the math simpler, but makes it weird if the content is scrolled
        // while animating.
        // TODO calculate the bounds dynamically and account for transforms during the animation
        //  instead.
        val coords = coordinates?.takeIf { it.isAttached } ?: return
        val childCoords = activeChild?.coordinates?.takeIf { it.isAttached } ?: return
        activeChildBounds = coords.localBoundingBoxOf(childCoords, clipBounds = false)

        coroutineScope.launch {
            zoomFactorAnimatable.animateTo(1f, zoomAnimationSpec)
            zoomDirection = null
        }
    }

    override fun zoomOut() {
        check(activeChild != null) { "Already zoomed to parent." }
        zoomDirection = ZoomDirection.ZoomingOut
        coroutineScope.launch {
            zoomFactorAnimatable.animateTo(0f, zoomAnimationSpec)
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
    var coordinates: LayoutCoordinates? = null

    /**
     * A [movableContentOf] wrapper that allows all state inside the child's composable to be moved
     * from being composed from inside the content of a [FractalNavHost] to being the root of
     * the host.
     */
    private val movableContent = movableContentOf { modifier: Modifier ->
        FractalNavHost(modifier) {
            val scope = remember(this) { ChildScope(this) }
            _content?.invoke(scope)
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

    private inner class ChildScope(navScope: FractalNavScope) : FractalNavScope by navScope,
        FractalNavChildScope {
        override val isActive: Boolean by derivedStateOf { parent.activeChild === this@FractalChild }
        override val isFullyZoomedIn: Boolean by derivedStateOf { isActive && parent.zoomFactor == 1f }
        override val zoomFactor: Float by derivedStateOf { if (isActive) parent.zoomFactor else 0f }
        override val zoomDirection: ZoomDirection?
            get() = if (isActive) parent.zoomDirection else null

        override fun zoomToParent() {
            parent.zoomOut()
        }
    }
}