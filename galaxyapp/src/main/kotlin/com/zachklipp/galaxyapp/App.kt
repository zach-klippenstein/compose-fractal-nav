package com.zachklipp.galaxyapp

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.zachklipp.fractalnav.FractalNavHost
import com.zachklipp.fractalnav.FractalNavState

@Composable
@Preview
fun App(navState: FractalNavState = remember { FractalNavState() }) {
    val universeInfo = remember { UniverseInfo() }
    MaterialTheme(colors = darkColors()) {
        Surface {
            FractalNavHost(
                state = navState,
                modifier = Modifier.fillMaxSize()
            ) {
                Universe(universeInfo)
            }
        }
    }
}