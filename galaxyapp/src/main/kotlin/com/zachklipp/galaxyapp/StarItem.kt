package com.zachklipp.galaxyapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.unit.dp
import com.zachklipp.fractalnav.FractalNavChildScope
import com.zachklipp.fractalnav.FractalNavScope

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
    var planetUnderFinger by remember { mutableIntStateOf(-1) }

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = CenterHorizontally
    ) {
        if (isActive) {
            Row(
                verticalAlignment = CenterVertically,
                modifier = Modifier
                    .scaleLayoutByZoomFactor()
                    .alphaByZoomFactor()
            ) {
                BackButton()
                Spacer(Modifier.size(8.dp))
                Text("The ${star.name} System", Modifier.weight(1f), maxLines = 1)
            }
            Text(
                if (planetUnderFinger in planets.indices) {
                    "${planets[planetUnderFinger].name}, release to open."
                } else {
                    "Tap or drag on the planets."
                },
                maxLines = 1,
                modifier = Modifier
                    .scaleLayoutByZoomFactor()
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

        if (isActive) {
            InfoText(
                text = star.description,
                modifier = Modifier
                    .fillExpandedWidth()
                    .alphaByZoomFactor()
            )
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