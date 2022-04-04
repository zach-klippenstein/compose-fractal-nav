package com.zachklipp.galaxyapp

import androidx.activity.compose.BackHandler
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import com.zachklipp.fractalnav.FractalNavChildScope
import com.zachklipp.fractalnav.ZoomDirection

@Composable
fun FractalNavChildScope.BackButton() {
    if (isFullyZoomedIn || zoomDirection == ZoomDirection.ZoomingIn) {
        BackHandler {
            zoomToParent()
        }
    }

    IconButton(onClick = { zoomToParent() }) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
    }
}