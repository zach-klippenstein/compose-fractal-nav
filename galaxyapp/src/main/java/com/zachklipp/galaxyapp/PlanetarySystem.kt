package com.zachklipp.galaxyapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput

/**
 * An animated, scientifically-inaccurate model of a star and its planets.
 */
@Composable
fun <P> PlanetarySystem(
    star: @Composable () -> Unit,
    planets: List<P>,
    modifier: Modifier = Modifier,
    planetDistributionScale: () -> Float = { 1f },
    onPlanetTouched: ((Int) -> Unit)? = null,
    onPlanetSelected: ((Int) -> Unit)? = null,
    planetContent: @Composable (P) -> Unit
) {
    val planetRadii = remember { mutableStateListOf<Float>() }
    var selectedPlanet by remember { mutableStateOf(-1) }

    // Keep the size of the list of radii in sync with the actual planets list so when the
    // onRadiusMeasured callback fires it's always exactly the right size.
    SideEffect {
        if (planetRadii.size > planets.size) {
            planetRadii.removeRange(planets.size, planetRadii.size)
        } else if (planets.size > planetRadii.size) {
            for (i in planetRadii.size until planets.size) {
                planetRadii.add(0f)
            }
        }
    }

    val drawOrbitsModifier = Modifier.drawBehind {
        planetRadii.forEach { orbitRadius ->
            drawCircle(
                Color.Gray,
                radius = orbitRadius,
                style = Stroke(),
                alpha = 0.5f * planetDistributionScale()
            )

            if (selectedPlanet in planetRadii.indices) {
                drawCircle(
                    Color.LightGray,
                    radius = planetRadii[selectedPlanet],
                    alpha = 0.5f
                )
            }
        }
    }

    val inputModifier = if (onPlanetSelected != null && planets.isNotEmpty()) {
        Modifier.pointerInput(planets, onPlanetSelected, onPlanetTouched) {
            forEachGesture {
                awaitPointerEventScope {
                    var change: PointerInputChange? = awaitFirstDown()
                    while (change != null && change.pressed) {
                        val totalRadius = size.width / 2f
                        val radius = (change.position - Offset(
                            totalRadius,
                            totalRadius
                        )).getDistance() / totalRadius
                        selectedPlanet = if (radius <= 0.3f || radius > 1f) {
                            -1
                        } else {
                            (planets.size * (radius - 0.3f) / .7f / planetDistributionScale()).toInt()
                        }
                        onPlanetTouched?.invoke(selectedPlanet)
                        change.consumePositionChange()
                        change = awaitDragOrCancellation(change.id)
                    }

                    // Pointer was either raised or cancelled.
                    if (change != null && selectedPlanet != -1) {
                        onPlanetSelected(selectedPlanet)
                    }
                    selectedPlanet = -1
                }
            }
        }
    } else Modifier

    RadialLayout(
        modifier
            .then(drawOrbitsModifier)
            .then(inputModifier)
    ) {
        val transition = rememberInfiniteTransition()

        val starAngle by transition.animateRotation(20_000)
        val starTwinkleScale by transition.animateFloat(
            initialValue = 0.99f,
            targetValue = 1.01f,
            animationSpec = infiniteRepeatable(
                tween(500),
                repeatMode = RepeatMode.Reverse
            )
        )
        Box(
            Modifier
                // Sun is twice as big as planets.
                .weight { 2f }
                .centerOffsetPercent { 0f }
                .graphicsLayer {
                    val scale = 0.5f + 0.5f * (1f - planetDistributionScale())
                    scaleX = scale
                    scaleY = scale
                    rotationZ = -starAngle
                }
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        // Layer the star so that it looks glowy.
                        scale(starTwinkleScale) {
                            this@onDrawWithContent.drawContent()
                        }
                    }
                }
        ) {
            star()
        }

        planets.forEachIndexed { i, planet ->
            val orbitAngle by transition.animateRotation(2_000 * (i + 1))
            val rotationAngle by transition.animateRotation(3_000)
            Box(
                Modifier
                    .weight(planetDistributionScale)
                    .scale(.5f)
                    .centerOffsetPercent {
                        0.3f + 0.7f * (i / planets.size.toFloat() * planetDistributionScale())
                    }
                    .onRadiusMeasured {
                        planetRadii[i] = it
                    }
                    .angleDegrees { -orbitAngle }
                    .graphicsLayer {
                        rotationZ = rotationAngle
                    }
            ) {
                planetContent(planet)
            }
        }
    }
}

@Composable
private fun InfiniteTransition.animateRotation(duration: Int) = animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
        tween(
            durationMillis = duration,
            easing = LinearEasing
        )
    )
)