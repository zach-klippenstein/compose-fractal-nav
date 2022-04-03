package com.zachklipp.galaxyapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.unit.dp
import com.zachklipp.fractalnav.FractalNavChildScope
import com.zachklipp.fractalnav.FractalNavScope

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

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun FractalNavChildScope.GalaxyChild(galaxy: Galaxy, universeInfo: UniverseInfo) {
    Column {
        Box {
            GalaxyImage(galaxy, Modifier.fillMaxWidth())
            AnimatedContent(isFullyZoomedIn) { showBack ->
                if (showBack) {
                    BackButton()
                }
            }
        }

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
private fun GalaxyImage(galaxy: Galaxy, modifier: Modifier) {
    NetworkImage(
        url = galaxy.imageUrl,
        contentDescription = "Image of ${galaxy.name}",
        modifier = modifier,
        blendMode = BlendMode.Screen,
    )
}

