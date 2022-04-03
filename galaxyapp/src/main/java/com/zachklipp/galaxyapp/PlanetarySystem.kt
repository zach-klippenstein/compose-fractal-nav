package com.zachklipp.galaxyapp

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val StarLayoutId = Any()

@Composable
fun PlanetarySystem(
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