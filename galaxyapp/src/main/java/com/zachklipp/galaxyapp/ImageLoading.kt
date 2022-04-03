package com.zachklipp.galaxyapp

import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    blendMode: BlendMode? = null,
    loadingContent: @Composable (Float) -> Unit = { CircularProgressIndicator(it) },
    errorContent: @Composable (Throwable) -> Unit = { Text(it.message ?: "Error") }
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier.then(blendMode?.let(Modifier::blendMode) ?: Modifier)
    )
//    val painterResource = lazyPainterResource(data = url)
//    @Suppress("NAME_SHADOWING")
//    Crossfade(
//        targetState = painterResource,
//        modifier = modifier
//    ) { painterResource ->
//        when (painterResource) {
//            is Resource.Loading -> loadingContent(painterResource.progress)
//            is Resource.Failure -> errorContent(painterResource.exception)
//            is Resource.Success -> {
//                Image(
//                    painter = painterResource.value,
//                    contentDescription = contentDescription,
//                    modifier = blendMode?.let(Modifier::blendMode) ?: Modifier
//                )
//            }
//        }
//    }
}

fun Modifier.blendMode(mode: BlendMode): Modifier = drawWithContent {
    drawIntoCanvas {
        it.withSaveLayer(
            bounds = Rect(Offset.Zero, size),
            paint = Paint().apply {
                blendMode = mode
            }
        ) {
            drawContent()
        }
    }
}