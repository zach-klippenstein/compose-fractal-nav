package com.zachklipp.galaxyapp

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * An animated, scientifically-inaccurate model of a star and its planets.
 *
 * @param orbitScale A function that returns a value between 0 and 1 that controls how much the
 * planet orbits are scaled around the center. 0 means all planets will be pinned to the center.
 * @param orbitAnimationScale A function that returns a value between 0 and 1 that controls the
 * speed of the orbits.
 */
@Composable
fun <P> PlanetarySystem(
    star: @Composable () -> Unit,
    planets: List<P>,
    modifier: Modifier = Modifier,
    orbitScale: () -> Float = { 1f },
    orbitAnimationScale: () -> Float = { 1f },
    onPlanetTouched: ((Int) -> Unit)? = null,
    onPlanetSelected: ((Int) -> Unit)? = null,
    planetContent: @Composable (P) -> Unit
) {
    val planetRadii = remember { mutableStateListOf<Float>() }
    var selectedPlanet by remember { mutableIntStateOf(-1) }

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
                alpha = 0.5f * orbitScale()
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
        val updatedOnPlanetTouched by rememberUpdatedState(onPlanetTouched)
        val updatedOnPlanetSelected by rememberUpdatedState(onPlanetSelected)
        Modifier.pointerInput(planets) {
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
                            (planets.size * (radius - 0.3f) / .7f / orbitScale()).toInt()
                        }
                        updatedOnPlanetTouched?.invoke(selectedPlanet)
                        change.consumePositionChange()
                        change = awaitDragOrCancellation(change.id)
                    }

                    // Pointer was either raised or cancelled.
                    if (change != null && selectedPlanet != -1) {
                        updatedOnPlanetSelected(selectedPlanet)
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
        val transition = rememberInfiniteTransition(label = "")

        val starAngle by animateRotation(20_000)
        val starTwinkleScale by transition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                tween(500),
                repeatMode = RepeatMode.Reverse
            ), label = ""
        )
        Box(
            Modifier
                // Make the star twice as big as the planets.
                .weight { 2f }
                .centerOffsetPercent { 0f }
                .scaleConstraints { 0.5f + 0.5f * (1f - orbitScale()) }
                .graphicsLayer {
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
                },
            propagateMinConstraints = true
        ) {
            star()
        }

        planets.forEachIndexed { i, planet ->
            val orbitAngle by animateRotation(
                duration = 2_000 * (i + 1),
                scale = orbitAnimationScale
            )
            Box(
                Modifier
                    .weight(orbitScale)
                    .centerOffsetPercent {
                        0.3f + 0.7f * (i / planets.size.toFloat() * orbitScale())
                    }
                    .onRadiusMeasured {
                        planetRadii[i] = it
                    }
                    .angleDegrees { -orbitAngle }
                    .scaleConstraints { 0.5f }
            ) {
                planetContent(planet)
            }
        }
    }
}

/**
 * Insets and centers a layout by a [factor] of its constraints.
 * Works around the [scale] modifier not being used correctly in calculations.
 */
private fun Modifier.scaleConstraints(factor: () -> Float): Modifier = layout { m, c ->
    val scale = factor()
    if (scale == 0f) {
        // Don't measure or place if it won't take any space anyway.
        return@layout layout(0, 0) {}
    }

    val constraints = Constraints(
        minWidth = (c.minWidth * scale).roundToInt(),
        minHeight = (c.minHeight * scale).roundToInt(),
        maxWidth = (c.maxWidth * scale).roundToInt(),
        maxHeight = (c.maxHeight * scale).roundToInt()
    )
    val p = m.measure(constraints)
    layout(c.maxWidth, c.maxHeight) {
        val center = IntOffset(c.maxWidth, c.maxHeight) / 2f
        val pCenter = IntOffset(p.width, p.height) / 2f
        p.place(center - pCenter)
    }
}

/**
 * Returns a value that will animate continuously between 0 and 360 in [duration] millis * [scale].
 * The rotation angle is saved in the instance state.
 */
@Composable
fun animateRotation(duration: Int, scale: () -> Float = { 1f }): State<Float> {
    val angle = rememberSaveable { mutableFloatStateOf(0f) }
    val running by remember { derivedStateOf { scale() != 0f } }
    if (running) {
        LaunchedEffect(duration) {
            var previousTime: Long = AnimationConstants.UnspecifiedTime
            val degreesPerMilli = 360f / duration
            while (true) {
                withInfiniteAnimationFrameMillis { frame ->
                    if (previousTime == AnimationConstants.UnspecifiedTime) previousTime = frame
                    val angleChange =
                        angle.floatValue + degreesPerMilli * (frame - previousTime) * scale()
                    angle.floatValue = angleChange.mod(360f)
                    previousTime = frame
                }
            }
        }
    }
    return angle
}