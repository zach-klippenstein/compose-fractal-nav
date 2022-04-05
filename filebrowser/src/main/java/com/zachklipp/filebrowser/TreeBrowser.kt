package com.zachklipp.filebrowser

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.zachklipp.fractalnav.*
import com.zachklipp.fractalnav.ZoomDirection.ZoomingIn

@JvmInline
value class TreeBrowserState private constructor(
    internal val navState: FractalNavState
) {
    constructor() : this(FractalNavState())
}

interface TreeBrowserScope<N> : FractalNavChildScope {
    @Composable
    fun ChildNode(node: N)
}

sealed interface ThumbnailType {
    data class Leaf(val content: @Composable () -> Unit) : ThumbnailType
    object Parent : ThumbnailType
}

@Composable
fun <N> TreeBrowser(
    rootNode: N,
    nodeKey: (N) -> String,
    childrenContent: @Composable TreeBrowserScope<N>.(N) -> Unit,
    nodeContent: @Composable (
        node: N,
        onClick: (() -> Unit)?,
        thumbnail: @Composable (Modifier) -> Unit
    ) -> Unit,
    thumbnail: @Composable (N) -> ThumbnailType,
    modifier: Modifier = Modifier,
    state: TreeBrowserState = remember { TreeBrowserState() }
) {
    FractalNavHost(modifier = modifier, state = state.navState) {
        ParentNode(rootNode, nodeKey, childrenContent, nodeContent, thumbnail)
    }
}

@Composable
private fun <N> FractalNavScope.ParentNode(
    node: N,
    nodeKey: (N) -> String,
    childrenContent: @Composable TreeBrowserScope<N>.(N) -> Unit,
    nodeContent: @Composable (
        node: N,
        onClick: (() -> Unit)?,
        thumbnail: @Composable (Modifier) -> Unit
    ) -> Unit,
    thumbnail: @Composable (N) -> ThumbnailType,
) {
    val scope = object : TreeBrowserScope<N>, FractalNavChildScope by fakeChildScope(this) {
        @Composable
        override fun ChildNode(node: N) {
            val thumbnailType = thumbnail(node)
            nodeContent(node, onClick = when (thumbnailType) {
                is ThumbnailType.Leaf -> null
                ThumbnailType.Parent -> {
                    { zoomToChild(nodeKey(node)) }
                }
            }) { modifier ->
                Crossfade(thumbnailType as ThumbnailType, modifier
                    .fillMaxSize()
                    .wrapContentSize()) { thumbnailType ->
                    when (thumbnailType) {
                        ThumbnailType.Parent -> {
                            FractalNavChild(
                                key = nodeKey(node),
                                modifier = Modifier
                            ) {
                                val childScope = this
                                childScope.ParentNode(
                                    node,
                                    nodeKey,
                                    childrenContent,
                                    nodeContent,
                                    thumbnail
                                )
                                if (isFullyZoomedIn || zoomDirection == ZoomingIn) {
                                    BackHandler {
                                        zoomToParent()
                                    }
                                }
                            }
                        }
                        is ThumbnailType.Leaf -> {
                            thumbnailType.content()
                        }
                    }
                }
            }
        }
    }
    scope.childrenContent(node)
}

private fun fakeChildScope(fractalNavScope: FractalNavScope): FractalNavChildScope {
    return (fractalNavScope as? FractalNavChildScope)
        ?: object : FractalNavChildScope,
            FractalNavScope by fractalNavScope {
            override val isActive: Boolean
                get() = true
            override val isFullyZoomedIn: Boolean
                get() = true
            override val zoomFactor: Float
                get() = 1f
            override val zoomDirection: ZoomDirection?
                get() = null

            override fun zoomToParent() {}
            override fun Modifier.fillExpandedWidth(): Modifier = this
        }
}
