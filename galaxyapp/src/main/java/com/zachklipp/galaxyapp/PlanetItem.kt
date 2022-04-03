package com.zachklipp.galaxyapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
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
        PlanetImage(planet, modifier)
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