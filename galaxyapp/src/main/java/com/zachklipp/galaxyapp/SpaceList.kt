package com.zachklipp.galaxyapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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