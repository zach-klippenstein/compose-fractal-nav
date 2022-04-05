package com.zachklipp.galaxyapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Dimension

/**
 * Thin wrapper around external image loader library.
 *
 * @param key An arbitrary value that will cause the image request to be re-started when it changes
 * between compositions. Use to workaround the Coil bug where the image is initially loaded into a
 * very small space and then that space grows.
 */
@Composable
fun NetworkImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillWidth,
    alignment: Alignment = Alignment.Center,
    blendMode: BlendMode? = null,
    colorFilter: ColorFilter? = null,
    cacheOriginal: Boolean = false,
) {
    if (cacheOriginal) {
        // Warm the cache with the image as large to workaround a Coil bug that doesn't reload the
        // image at a higher resolution after loading it for a very small size.
        val context = LocalContext.current
        LaunchedEffect(context) {
            context.imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(url)
                    .size(Dimension.Original, Dimension.Original)
                    .build()
            )
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        colorFilter = colorFilter,
        alignment = alignment,
        modifier = modifier.then(blendMode?.let(Modifier::blendMode) ?: Modifier),
    )
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