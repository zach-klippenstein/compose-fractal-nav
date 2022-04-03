package com.zachklipp.galaxyapp

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zachklipp.fractalnav.FractalNavScope

@Composable
fun FractalNavScope.Universe(universeInfo: UniverseInfo) {
    val galaxies by universeInfo.galaxies.collectAsState()

    Column(horizontalAlignment = CenterHorizontally) {
        Text("Galaxies", Modifier.padding(8.dp))

        @Suppress("NAME_SHADOWING")
        Crossfade(galaxies) { galaxies ->
            if (galaxies == null) {
                CircularProgressIndicator()
            } else {
                SpaceList(galaxies, Modifier.verticalScroll(rememberScrollState())) { galaxy ->
                    GalaxyItem(galaxy, universeInfo, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

