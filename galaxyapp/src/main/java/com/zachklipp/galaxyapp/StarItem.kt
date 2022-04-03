package com.zachklipp.galaxyapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            FractalNavChild(
                star.fractalKey,
                Modifier
                    .size(64.dp)
                    .wrapContentSize()
            ) {
                StarChild(star, universeInfo)
            }
            Text(star.name)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun FractalNavChildScope.StarChild(star: Star, universeInfo: UniverseInfo) {
    PlanetarySystem(
        planetDistributionScale = zoomFactor,
        star = {
            Column(horizontalAlignment = CenterHorizontally) {
                Box(
                    propagateMinConstraints = true,
                    contentAlignment = Center,
                    modifier = Modifier.weight(1f)
                ) {
                    StarImage(star, Modifier)
                    AnimatedContent(isFullyZoomedIn) { showBack ->
                        if (showBack) {
                            BackButton()
                        }
                    }
                }
            }
        }
    ) {
        if (isActive) {
            val planets by remember(star, universeInfo) { universeInfo.getPlanets(star) }
                .collectAsState()
            planets?.forEach { planet ->
                Column(horizontalAlignment = CenterHorizontally) {
                    PlanetImage(planet, Modifier.weight(1f))
                }
            }
        }
    }
}

private val StarLayoutId = Any()

@Composable
private fun PlanetarySystem(
    star: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    planetSpacing: Dp = 8.dp,
    planetDistributionScale: Float = 1f,
    planets: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = {
            Box(
                Modifier.layoutId(StarLayoutId),
                propagateMinConstraints = true
            ) {
                star()
            }
            planets()
        }
    ) { measurables, constraints ->
        val (starMeasurables, planetMeasurables) = measurables.partition { it.layoutId === StarLayoutId }
        val starMeasurable = starMeasurables.single()

        val minDimension = minOf(constraints.maxWidth, constraints.maxHeight)
        // Star takes up space of 2 planets.
        val planetCount = planetMeasurables.size
        val totalSpacing = planetCount * planetSpacing.roundToPx()
        val availableSpaceForPlanets = if (totalSpacing > minDimension / 2) {
            minDimension / 2
        } else {
            minDimension / 2 - totalSpacing
        }
        val sizePerPlanet: Int = availableSpaceForPlanets / (planetCount + 2)
        val planetConstraints = Constraints(maxWidth = sizePerPlanet, maxHeight = sizePerPlanet)
        val planetPlaceables = planetMeasurables.map {
            it.measure(planetConstraints)
        }

        // Only showing star, take full space.
        val starSize = if (planetCount == 0) minDimension else {
            lerp(minDimension, sizePerPlanet * 2, planetDistributionScale)
        }
        val starConstraints = Constraints(maxWidth = starSize, maxHeight = starSize)
        val starPlaceable = starMeasurable.measure(starConstraints)

        layout(minDimension, minDimension) {
            val center = IntOffset(minDimension / 2, minDimension / 2)

            // Center the star.
            starPlaceable.place(
                center - IntOffset(starPlaceable.width / 2, starPlaceable.height / 2)
            )

            // Place planets around it.
            planetPlaceables.forEachIndexed { i, placeable ->
                val x = (i + 2) * (sizePerPlanet + planetSpacing.roundToPx())
                val scaledX = (x * planetDistributionScale).roundToInt()
                placeable.place(
                    center + IntOffset(scaledX, 0) - IntOffset(
                        placeable.width / 2,
                        placeable.height / 2
                    )
                )
            }
        }
    }
}

@Composable
private fun StarImage(star: Star, modifier: Modifier) {
    NetworkImage(
        url = star.imageUrl,
        contentDescription = "Image of ${star.name}",
        modifier = modifier,
        blendMode = BlendMode.Screen,
    )
}

@Composable
private fun PlanetImage(planet: Planet, modifier: Modifier) {
    NetworkImage(
        url = planet.imageUrl,
        contentDescription = "Image of ${planet.name}",
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
