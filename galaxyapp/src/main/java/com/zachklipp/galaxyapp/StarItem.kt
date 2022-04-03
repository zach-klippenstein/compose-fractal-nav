package com.zachklipp.galaxyapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
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
                    .clipToBounds()
            ) {
                StarChild(star, universeInfo)
            }
            // Set maxlines to 1 to avoid wrapping when close to fully zoomed-out.
            Text(star.name, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun FractalNavChildScope.StarChild(star: Star, universeInfo: UniverseInfo) {
    val planets: List<Planet> = if (isActive) {
        remember(star, universeInfo) { universeInfo.getPlanets(star) }
            .collectAsState()
            .value ?: emptyList()
    } else {
        emptyList()
    }

    PlanetarySystem(
        planetDistributionScale = { zoomFactor },
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
        },
        planets = planets
    ) { planet ->
        Column(horizontalAlignment = CenterHorizontally) {
            PlanetImage(planet, Modifier.weight(1f).background(Color.Black, CircleShape))
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
        // The planets start off being measured only a few pixels, so Coil will cache that scaled-
        // down image and never refresh it even when the size grows unless we explicitly cache the
        // original size.
        cacheOriginal = true
    )
}

fun lerp(i1: Int, i2: Int, fraction: Float): Int {
    return (i1 + (i2 - i1) * fraction).roundToInt()
}

fun lerp(f1: Float, f2: Float, fraction: Float): Float {
    return f1 + (f2 - f1) * fraction
}
