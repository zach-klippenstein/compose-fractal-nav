package com.zachklipp.galaxyapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FractalNavChildScope.GalaxyChild(galaxy: Galaxy, universeInfo: UniverseInfo) {
    val scrollState = rememberScrollState()
    val bringHeroIntoViewRequester = remember { BringIntoViewRequester() }

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
        verticalArrangement = spacedBy(8.dp),
        modifier = if (isActive) Modifier.verticalScroll(scrollState) else Modifier
    ) {
        GalaxyHero(
            galaxy,
            showControls = isFullyZoomedIn || zoomDirection == ZoomingIn,
            modifier = if (isActive) {
                Modifier.bringIntoViewRequester(bringHeroIntoViewRequester)
            } else Modifier
        )

        if (isActive) {
            val stars = remember(galaxy, universeInfo) { universeInfo.getStars(galaxy) }
                .collectAsState().value ?: emptyList()

            InfoText(
                text = galaxy.description,
                modifier = Modifier
                    .fillExpandedWidth()
                    .alphaByZoomFactor()
            )

            if (stars.isNotEmpty()) {
                ListHeader("Stars")
                SpaceList(stars) { star ->
                    StarItem(star, universeInfo, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun FractalNavChildScope.GalaxyHero(
    galaxy: Galaxy,
    showControls: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        GalaxyImage(
            galaxy, Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    clip = true
                    shape = RoundedCornerShape(10.dp * (1f - zoomFactor))
                }
        )
        AnimatedVisibility(showControls, Modifier.align(TopStart)) {
            BackButton()
        }
        AnimatedVisibility(
            showControls,
            modifier = Modifier.align(Center),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                "The ${galaxy.name} Galaxy",
                style = MaterialTheme.typography.h4
                    .copy(
                        shadow = Shadow(blurRadius = 5f),
                        textAlign = TextAlign.Center,
                    ),
                modifier = Modifier
                    .alphaByZoomFactor()
                    .scaleByZoomFactor()
                    .fillExpandedWidth()
                    .padding(8.dp)
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
        alignment = TopCenter,
        cacheOriginal = true,
    )
}

