package com.zachklipp.fractalnav

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates

internal interface FractalParent : FractalNavScope {
    val activeChild: FractalChild?
    val zoomDirection: ZoomDirection?
    val zoomAnimationSpecFactory: () -> AnimationSpec<Float>

    fun zoomOut()
}

@OptIn(ExperimentalFoundationApi::class)
internal class FractalChild(
    val key: String,
    private val parent: FractalParent
) {
    private val childState = FractalNavState() as FractalNavStateImpl
    private val childScope = ChildScope(childState)
    private var _content: (@Composable FractalNavChildScope.() -> Unit)? by mutableStateOf(null)
    var placeholderCoordinates: LayoutCoordinates? = null
    val bringIntoViewRequester = BringIntoViewRequester()

    /**
     * A [movableContentOf] wrapper that allows all state inside the child's composable to be moved
     * from being composed from inside the content of a [FractalNavHost] to being the root of
     * the host.
     */
    private val movableContent = movableContentOf { modifier: Modifier ->
        FractalNavHost(
            state = childState,
            modifier = modifier,
            zoomAnimationSpecFactory = { parent.zoomAnimationSpecFactory() }
        ) {
            _content?.invoke(childScope)
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

    private inner class ChildScope(private val state: FractalNavStateImpl) :
        FractalNavScope by state,
        FractalNavChildScope {
        override val isActive: Boolean by derivedStateOf {
            parent.activeChild === this@FractalChild
        }
        override val isFullyZoomedIn: Boolean by derivedStateOf {
            isActive && parent.childZoomFactor == 1f
        }
        override val zoomFactor: Float by derivedStateOf {
            if (isActive) parent.childZoomFactor else 0f
        }
        override val zoomDirection: ZoomDirection?
            get() = if (isActive) parent.zoomDirection else null

        override fun zoomToChild(key: String) {
            // An inactive child should never have zoomed-in children, so if we're about to become
            // inactive we need to switch to zooming in first.
            if (zoomDirection == ZoomDirection.ZoomingOut) {
                parent.zoomToChild(this@FractalChild.key)
            }
            state.zoomToChild(key)
        }

        override fun zoomToParent() {
            if (state.hasActiveChild) {
                // A zoomed-out child should never have zoomed-in descendents.
                state.zoomOut()
            }
            parent.zoomOut()
        }
    }
}