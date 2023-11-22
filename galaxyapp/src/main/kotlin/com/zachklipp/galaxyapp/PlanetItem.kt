package com.zachklipp.galaxyapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zachklipp.fractalnav.FractalNavChildScope
import com.zachklipp.fractalnav.ZoomDirection

@Composable
fun FractalNavChildScope.PlanetItem(
    planet: Planet,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = spacedBy(8.dp)
    ) {
        PlanetHero(
            planet,
            showControls = isFullyZoomedIn || zoomDirection == ZoomDirection.ZoomingIn
        )

        if (isActive) {
            InfoText(
                text = planet.description,
                modifier = Modifier
                    .fillExpandedWidth()
                    .alphaByZoomFactor()
            )
        }
    }
}

@Composable
private fun FractalNavChildScope.PlanetHero(
    planet: Planet,
    showControls: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        // Spin the planet when zoomed out, but slow it way down when zoomed in.
        val rotationAngle by animateRotation(3_000, scale = { 1f - zoomFactor * 0.9f })
        PlanetImage(
            planet = planet,
            // Darken the image a bit when zoomed in so the white text shows up better.
            tint = zoomFactor * 0.5f,
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotationAngle }
                // Since the images are drawn with the Screen blendmode, they're
                // translucent. We need an opaque background to block out the orbit
                // line.
                .background(MaterialTheme.colors.background, CircleShape)
                .drawWithContent {
                    drawIntoCanvas { }
                    drawContent()
                }
        )
        AnimatedVisibility(showControls, Modifier.align(Alignment.TopStart)) {
            BackButton()
        }
        AnimatedVisibility(
            showControls,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                planet.name,
                style = MaterialTheme.typography.h4
                    .copy(
                        shadow = Shadow(blurRadius = 5f),
                        textAlign = TextAlign.Center,
                    ),
                modifier = Modifier
                    .alphaByZoomFactor()
                    .scaleByZoomFactor()
                    .fillExpandedWidth()
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun PlanetImage(
    planet: Planet,
    modifier: Modifier,
    tint: Float
) {
    NetworkImage(
        url = planet.imageUrl,
        contentDescription = "Image of ${planet.name}",
        modifier = modifier,
        blendMode = BlendMode.Screen,
        colorFilter = ColorFilter.tint(Color.Black.copy(alpha = tint), BlendMode.Darken),
        // The planets start off being measured only a few pixels, so Coil will cache that scaled-
        // down image and never refresh it even when the size grows unless we explicitly cache the
        // original size.
        cacheOriginal = true
    )
}