package com.zachklipp.galaxyapp

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer

/**
 * An animated, scientifically-inaccurate model of a star and its planets.
 */
@Composable
fun <P> PlanetarySystem(
    star: @Composable () -> Unit,
    planets: List<P>,
    modifier: Modifier = Modifier,
    planetDistributionScale: () -> Float = { 1f },
    planetContent: @Composable (P) -> Unit
) {
    val planetRadii = remember { mutableStateListOf<Float>() }
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
        }
    }

    RadialLayout(modifier.then(drawOrbitsModifier)) {
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
                    .weight { planetDistributionScale() }
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