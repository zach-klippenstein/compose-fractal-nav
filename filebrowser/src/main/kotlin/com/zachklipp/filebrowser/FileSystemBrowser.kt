package com.zachklipp.filebrowser

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zachklipp.fractalnav.ZoomDirection.ZoomingOut
import com.zachklipp.fractalnav.lerp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okio.FileMetadata
import okio.FileSystem
import okio.Path

@Composable
fun FileSystemBrowser(
    root: Path,
    fileSystem: FileSystem,
    modifier: Modifier = Modifier,
    state: TreeBrowserState = remember { TreeBrowserState() }
) {
    TreeBrowser<Path?>(
        rootNode = root,
        modifier = modifier,
        state = state,
        nodeKey = { it.toString() },
        childrenContent = { path ->
            FileList(path, fileSystem)
        },
        nodeContent = { path, onClick, thumbnail ->
            FileCard(path, onClick, thumbnail)
        },
        thumbnail = { path ->
            fileThumbnailType(path, fileSystem)
        },
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FileCard(
    path: Path?,
    onClick: (() -> Unit)?,
    thumbnail: @Composable (Modifier) -> Unit
) {
    val context = LocalContext.current
    Card(onClick = onClick ?: {
        Toast.makeText(context, "File clicked: $path", Toast.LENGTH_SHORT).show()
    }) {
        Row(
            horizontalArrangement = spacedBy(8.dp),
            verticalAlignment = CenterVertically
        ) {
            thumbnail(Modifier.size(64.dp))
            Text(
                path?.name.orEmpty(),
                Modifier
                    .weight(1f)
                    .then(if (path == null) Modifier.background(Color.LightGray) else Modifier)
            )
        }
    }
}

private val LoadingThumbnail = ThumbnailType.Leaf {
    CircularProgressIndicator(
        Modifier
            .fillMaxSize()
            .wrapContentSize()
    )
}
private val ErrorThumbnail = ThumbnailType.Leaf {
    Icon(Icons.Default.Warning, contentDescription = "error loading thumbnail")
}

@Composable
private fun fileThumbnailType(path: Path?, fileSystem: FileSystem): ThumbnailType {
    return produceState<ThumbnailType>(LoadingThumbnail, path) {
        if (path == null) return@produceState
        withContext(Dispatchers.IO) {
            supervisorScope {
                val metadata = async { fileSystem.metadata(path) }
                val children = async { fileSystem.list(path) }
                val result = runCatching { Pair(metadata.await(), children.await()) }.getOrNull()

                value = if (result == null) {
                    ErrorThumbnail
                } else if (result.second.isEmpty()) {
                    ThumbnailType.Leaf {
                        FilePreview(path, result.first, fileSystem)
                    }
                } else {
                    ThumbnailType.Parent
                }
            }
        }
    }.value
}

@Composable
private fun TreeBrowserScope<Path?>.FileList(
    path: Path?,
    fileSystem: FileSystem,
    initialChildren: List<Path>? = null,
) {
    var childCount by rememberSaveable { mutableStateOf(initialChildren?.size ?: -1) }
    var children: List<Path>? by rememberSaveable { mutableStateOf(initialChildren) }
    LaunchedEffect(path) {
        if (path == null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            children = fileSystem.list(path).also {
                childCount = it.size
            }
        }
    }
    val isLoaded by remember { derivedStateOf { childCount > -1 } }

    Crossfade(
        isLoaded,
        Modifier
            .fillMaxSize()
            .wrapContentSize()
    ) { isLoaded ->
        if (!isLoaded) {
            CircularProgressIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(),
                horizontalAlignment = CenterHorizontally
            ) {
                val showContent = isActive && zoomDirection != ZoomingOut
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        childCount.toString(),
                        fontSize = lerp(24f, LocalTextStyle.current.fontSize.value, zoomFactor).sp
                    )
                    AnimatedVisibility(showContent) {
                        Text(" files")
                    }
                }
                if (isActive && childCount > 0) {
                    FileGrid(
                        childCount,
                        Modifier
                            .scaleByZoomFactor()
                            .scaleLayoutByZoomFactor()
                            .fillExpandedWidth()
                            .fillMaxHeight()
                    ) {
                        ChildNode(children?.getOrNull(it))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilePreview(path: Path, metadata: FileMetadata, fileSystem: FileSystem) {
    Icon(
        Icons.Default.Email,
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize()
    )
}