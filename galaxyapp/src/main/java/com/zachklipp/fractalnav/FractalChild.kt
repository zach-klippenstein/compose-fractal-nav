package com.zachklipp.fractalnav

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates

internal interface FractalParent {
    val zoomFactor: Float
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
            isActive && parent.zoomFactor == 1f
        }
        override val zoomFactor: Float by derivedStateOf {
            if (isActive) parent.zoomFactor else 0f
        }
        override val zoomDirection: ZoomDirection?
            get() = if (isActive) parent.zoomDirection else null
        override val hasActiveChild: Boolean
            get() = state.activeChild != null
        override val childZoomFactor: Float
            get() = state.zoomFactor

        override fun zoomToParent() {
            parent.zoomOut()
        }
    }
}