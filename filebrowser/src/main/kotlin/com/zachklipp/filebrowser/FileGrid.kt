package com.zachklipp.filebrowser

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

@Composable
fun FileGrid(
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int) -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = spacedBy(8.dp)
    ) {
        repeat(ceil(count / 2f).toInt()) { i ->
            Row(horizontalArrangement = spacedBy(8.dp)) {
                Box(Modifier.weight(1f), propagateMinConstraints = true) {
                    content(i * 2)
                }
                Box(Modifier.weight(1f), propagateMinConstraints = true) {
                    if (i * 2 + 1 < count) {
                        content(i * 2 + 1)
                    } else {
                        Spacer(Modifier)
                    }
                }
            }
        }
    }

    // Lazy columns also crash
//    LazyColumn(
//        modifier = modifier,
//        contentPadding = PaddingValues(8.dp),
//        verticalArrangement = spacedBy(8.dp)
//    ) {
//        items(ceil(count / 2f).toInt()) { i ->
//            Row(horizontalArrangement = spacedBy(8.dp)) {
//                Box(Modifier.weight(1f), propagateMinConstraints = true) {
//                    content(i * 2)
//                }
//                Box(Modifier.weight(1f), propagateMinConstraints = true) {
//                    if (i * 2 + 1 < count) {
//                        content(i * 2 + 1)
//                    } else {
//                        Spacer(Modifier)
//                    }
//                }
//            }
//        }
//    }

    // Lazy grids crash when nested.
//    LazyVerticalGrid(
//        cells = GridCells.Adaptive(150.dp),
//        verticalArrangement = Arrangement.spacedBy(8.dp),
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        modifier = Modifier
//            .weight(1f)
//            .padding(8.dp)
//    ) {
//        items(childCount) { i ->
//            ChildNode(node = children?.getOrNull(i))
//        }
//    }
}