package com.zachklipp.fractalnav

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
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
    content: @Composable FractalNavScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val state = rememberSaveable(saver = FractalNavState.Saver) { FractalNavState(coroutineScope) }
    val contentStateHolder = rememberSaveableStateHolder()
    val rootModifier = modifier
        // TODO why isn't onPlaced working??
        .onPlaced { state.coordinates = it }
        .onGloballyPositioned { state.coordinates = it }
    val activeChild = state.activeChild

    if (state.isFullyZoomedIn) {
        checkNotNull(activeChild) { "Can't be fully zoomed in with no active child." }
        activeChild.MovableContent(rootModifier)
    } else {
        contentStateHolder.SaveableStateProvider("fractal-nav-host-$currentCompositeKeyHash") {
            Box(
                modifier = rootModifier.then(state.contentModifier),
                propagateMinConstraints = true
            ) {
                content(state)
            }
        }
    }
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

    /** Requests the parent of this [FractalNavChild] zoom it out and eventually deactivate it. */
    fun zoomToParent()
}

private interface FractalParent {
    val zoomFactor: Float
    val activeChild: FractalChild?

    fun zoomOut()
}

private class FractalNavState(private val coroutineScope: CoroutineScope) : FractalNavScope,
    FractalParent {
    private val children = mutableStateMapOf<String, FractalChild>()
    private val zoomAnimationSpec: AnimationSpec<Float> = tween(2_000)
    private val zoomFactorAnimatable = Animatable(0f)
    var coordinates: LayoutCoordinates? = null
    override val zoomFactor: Float by zoomFactorAnimatable.asState()
    val isFullyZoomedIn by derivedStateOf { zoomFactor == 1f }

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

    private val foo: Offset?
        get() = activeChildPivot?.let { pivot ->
            val parentCenter = coordinates!!.size.center.toOffset()
            val distanceToCenter = parentCenter - activeChildBounds!!.center
            Offset(
                zoomFactor * distanceToCenter.x,
                zoomFactor * distanceToCenter.y
            )
        }

    val contentModifier
        get() = if (activeChild != null && !isFullyZoomedIn) {
            Modifier.graphicsLayer {
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

    private val animateChildModifier = Modifier
        .graphicsLayer {
            // Cancel out the scale from the parent so the child stays the same size.
            val scaleTarget = activeChildScaleTarget
            scaleX = 1f / (1f + zoomFactor * scaleTarget.x)
            scaleY = 1f / (1f + zoomFactor * scaleTarget.y)
            transformOrigin = TransformOrigin(0f, 0f)
        }
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
                // Offset to cancel out the parent translation.
                placeable.place(
                    // Idk why, this code works on Desktop but not Android.
//                    Offset(
//                        x = zoomFactor * placeable.width / 2,
//                        y = zoomFactor * placeable.height / 2
//                    ).round()
                    // And this code works on Android.
                    Offset(
                        x = zoomFactor * activeChildBounds!!.left * 2,
                        y = zoomFactor * activeChildBounds!!.left * 2
                    ).round()
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

        // When the active child is fully zoomed-in, this composable will be removed from the
        // composition entirely and FractalNavHost will compose child.MovableContent itself.
        child.MovableContent(
            modifier
                // TODO why isn't onPlaced working?
                .onPlaced { child.coordinates = it }
                .onGloballyPositioned { child.coordinates = it }
                .then(if (activeChild === child) animateChildModifier else Modifier)
        )
    }

    override fun zoomToChild(key: String) {
        // Ignore requests when already zoomed.
        if (activeChild != null) return

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
        }
    }

    override fun zoomOut() {
        check(activeChild != null) { "Already zoomed to parent." }
        coroutineScope.launch {
            zoomFactorAnimatable.animateTo(0f, zoomAnimationSpec)
            activeChild = null
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

        override fun zoomToParent() {
            parent.zoomOut()
        }
    }
}