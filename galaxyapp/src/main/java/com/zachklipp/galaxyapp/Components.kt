package com.zachklipp.galaxyapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ListHeader(text: String) {
    Text(
        text,
        Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .wrapContentWidth()
    )
}

@Composable
fun <T> SpaceList(
    items: List<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Absolute.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            key(item) {
                content(item)
            }
        }
    }
}

// Note: Seems to crash when zooming out to items on the left side.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> SpaceGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    LazyVerticalGrid(
        cells = GridCells.Fixed(2),
        verticalArrangement = spacedBy(8.dp),
        horizontalArrangement = spacedBy(8.dp),
        modifier = modifier
            .padding(8.dp)
            .fillMaxSize(),
    ) {
        items(items) { item ->
            content(item)
        }
    }
}

@Composable
fun InfoText(text: String, modifier: Modifier = Modifier) {
    Text(
        text.lineSequence().joinToString(separator = " "),
        style = MaterialTheme.typography.body2,
        modifier = modifier.padding(horizontal = 8.dp)
    )
}