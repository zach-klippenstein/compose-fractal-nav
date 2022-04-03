package com.zachklipp.galaxyapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import com.zachklipp.fractalnav.FractalNavChildScope

@Composable
fun FractalNavChildScope.PlanetItem(
    planet: Planet,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.TopStart,
        propagateMinConstraints = true
    ) {
        if (isActive) {
            BackHandler {
                zoomToParent()
            }
        }

        // Spin the planet when zoomed out, but stop when zoomed in.
        val rotationAngle by animateRotation(3_000, scale = { 1f - zoomFactor })
        PlanetImage(planet, modifier.graphicsLayer {
            rotationZ = rotationAngle
        })
    }
}

@Composable
private fun PlanetImage(planet: Planet, modifier: Modifier) {
    NetworkImage(
        url = planet.imageUrl,
        contentDescription = "Image of ${planet.name}",
        modifier = modifier,
        blendMode = BlendMode.Screen,
        // The planets start off being measured only a few pixels, so Coil will cache that scaled-
        // down image and never refresh it even when the size grows unless we explicitly cache the
        // original size.
        cacheOriginal = true
    )
}