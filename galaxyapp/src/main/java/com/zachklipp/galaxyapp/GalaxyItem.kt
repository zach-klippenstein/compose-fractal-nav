package com.zachklipp.galaxyapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.zachklipp.fractalnav.FractalNavChildScope
import com.zachklipp.fractalnav.FractalNavScope
import com.zachklipp.fractalnav.ZoomDirection.ZoomingIn
import com.zachklipp.fractalnav.ZoomDirection.ZoomingOut

private val Galaxy.fractalKey get() = "galaxy-$name"

@Composable
fun FractalNavScope.GalaxyItem(
    galaxy: Galaxy,
    universeInfo: UniverseInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier.clickable { zoomToChild(galaxy.fractalKey) }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = spacedBy(8.dp),
            verticalAlignment = CenterVertically
        ) {
            FractalNavChild(
                galaxy.fractalKey,
                Modifier
                    .size(64.dp)
                    .wrapContentSize()
            ) {
                GalaxyChild(galaxy, universeInfo)
            }
            Text(galaxy.name)
        }
    }
}

@Composable
private fun FractalNavChildScope.GalaxyChild(galaxy: Galaxy, universeInfo: UniverseInfo) {
    val scrollState = rememberScrollState()

    // When zooming out, scroll back to the top, animating in coordination with the zoom.
    if (zoomDirection == ZoomingOut) {
        LaunchedEffect(scrollState) {
            val amountToScroll = scrollState.value
            snapshotFlow { zoomFactor }
                .collect {
                    scrollState.scrollTo(lerp(0, amountToScroll, it))
                }
        }
    }

    Column(
        modifier = if (isActive) Modifier.verticalScroll(scrollState) else Modifier
    ) {
        GalaxyHero(galaxy, showBack = isFullyZoomedIn || zoomDirection == ZoomingIn)

        if (isActive) {
            val stars by remember(galaxy, universeInfo) { universeInfo.getStars(galaxy) }
                .collectAsState()
            SpaceList(stars ?: emptyList()) { star ->
                StarItem(star, universeInfo, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FractalNavChildScope.GalaxyHero(galaxy: Galaxy, showBack: Boolean) {
    Box {
        GalaxyImage(galaxy, Modifier.fillMaxWidth())
        AnimatedVisibility(showBack, Modifier.align(TopStart)) {
            BackButton()
        }
        AnimatedVisibility(
            showBack,
            modifier = Modifier.align(Center),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                "The ${galaxy.name} Galaxy",
                maxLines = 1,
                style = LocalTextStyle.current.copy(shadow = Shadow(blurRadius = 5f))
            )
        }
    }
}

@Composable
private fun GalaxyImage(galaxy: Galaxy, modifier: Modifier) {
    NetworkImage(
        url = galaxy.imageUrl,
        contentDescription = "Image of ${galaxy.name}",
        modifier = modifier,
        blendMode = BlendMode.Screen,
        cacheOriginal = true
    )
}

