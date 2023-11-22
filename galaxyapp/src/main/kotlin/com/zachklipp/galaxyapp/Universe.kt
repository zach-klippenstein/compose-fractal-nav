package com.zachklipp.galaxyapp

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import com.zachklipp.fractalnav.FractalNavScope

@Composable
fun FractalNavScope.Universe(universeInfo: UniverseInfo) {
    val galaxies by universeInfo.galaxies.collectAsState()

    Column(horizontalAlignment = CenterHorizontally) {
        ListHeader("Galaxies")

        Crossfade(galaxies, label = "") { galaxies ->
            if (galaxies == null) {
                CircularProgressIndicator()
            } else {
                SpaceGrid(galaxies) { galaxy ->
                    GalaxyItem(galaxy, universeInfo, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

