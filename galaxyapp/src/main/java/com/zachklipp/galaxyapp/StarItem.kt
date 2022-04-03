package com.zachklipp.galaxyapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.unit.dp
import com.zachklipp.fractalnav.FractalNavChildScope
import com.zachklipp.fractalnav.FractalNavScope
import kotlin.math.roundToInt

private val Star.fractalKey get() = "star-$name"

@Composable
fun FractalNavScope.StarItem(
    star: Star,
    universeInfo: UniverseInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier.clickable { zoomToChild(star.fractalKey) }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.Absolute.spacedBy(8.dp),
            verticalAlignment = CenterVertically
        ) {
            FractalNavChild(
                star.fractalKey,
                Modifier
                    .size(64.dp)
                    .wrapContentSize()
                    .clipToBounds()
            ) {
                StarChild(star, universeInfo)
            }
            // Set maxlines to 1 to avoid wrapping when close to fully zoomed-out.
            Text(star.name, maxLines = 1)
        }
    }
}

@Composable
private fun FractalNavChildScope.StarChild(star: Star, universeInfo: UniverseInfo) {
    val planets: List<Planet> = if (isActive) {
        remember(star, universeInfo) { universeInfo.getPlanets(star) }
            .collectAsState()
            .value ?: emptyList()
    } else {
        emptyList()
    }
    var planetUnderFinger by remember { mutableStateOf(-1) }

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = CenterHorizontally
    ) {
        if (isActive) {
            Row(
                verticalAlignment = CenterVertically,
                modifier = Modifier
                    .scaleByZoomFactor()
                    .alphaByZoomFactor()
            ) {
                BackButton()
                Spacer(Modifier.size(8.dp))
                Text("The ${star.name} System", Modifier.weight(1f), maxLines = 1)
            }
            Text(
                if (planetUnderFinger != -1 && planets.isNotEmpty()) {
                    "${planets[planetUnderFinger].name}, release to open."
                } else {
                    "Tap or drag on the planets."
                },
                maxLines = 1,
                modifier = Modifier
                    .scaleByZoomFactor()
                    .alphaByZoomFactor()
            )
        }

        PlanetarySystem(
            star = { StarImage(star) },
            planets = planets,
            // When zooming in, animate the planets out from the center.
            orbitScale = { zoomFactor },
            // Slow then stop animation when a child is being zoomed in.
            orbitAnimationScale = { 1f - childZoomFactor },
            onPlanetTouched = {
                planetUnderFinger = it
            },
            onPlanetSelected = {
                planetUnderFinger = -1
                if (it in planets.indices) {
                    zoomToChild("planet-${planets[it].name}")
                }
            }
        ) { planet ->
            FractalNavChild(
                key = "planet-${planet.name}",
                modifier = Modifier
            ) {
                PlanetItem(planet)
            }
        }

        // Empty spacer just to take up the bottom slot in the column.
        Spacer(Modifier)
    }
}

@Composable
private fun StarImage(star: Star, modifier: Modifier = Modifier) {
    NetworkImage(
        url = star.imageUrl,
        contentDescription = "Image of ${star.name}",
        modifier = modifier,
        blendMode = BlendMode.Screen,
    )
}

fun lerp(i1: Int, i2: Int, fraction: Float): Int {
    return (i1 + (i2 - i1) * fraction).roundToInt()
}

fun lerp(f1: Float, f2: Float, fraction: Float): Float {
    return f1 + (f2 - f1) * fraction
}