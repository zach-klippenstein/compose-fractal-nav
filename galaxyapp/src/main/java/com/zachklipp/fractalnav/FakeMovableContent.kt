package com.zachklipp.fractalnav

import androidx.compose.runtime.Composable

// TODO replace with real movableContentOf when fixed. Currently it crashes when zooming out.
fun <P> movableContentOf(content: @Composable (P) -> Unit): @Composable (P) -> Unit {
    return content
}