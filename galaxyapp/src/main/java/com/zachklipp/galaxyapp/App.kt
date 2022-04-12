package com.zachklipp.galaxyapp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
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
//            MinimalRepro()
//            return@Surface
            FractalNavHost(
                state = navState,
                modifier = Modifier.fillMaxSize()
            ) {
                Universe(universeInfo)
            }
        }
    }
}

@Composable
fun MinimalRepro() {
    var extractContent by remember { mutableStateOf(false) }
    val innerState by remember { mutableStateOf(false) }
    var content: (@Composable () -> Unit)? by remember { mutableStateOf(null) }
    val movableContent = remember {
        movableContentOf {
            content?.invoke()
        }
    }

    Column {
        Row(verticalAlignment = CenterVertically) {
            Text("Text in lazy? ")
            Checkbox(extractContent, onCheckedChange = {
                extractContent = it
            })
        }

        LazyColumn {
            item {
                if (!extractContent) {
                    content = {
                        Text("Movable text: innerState=$innerState")
                    }
                    movableContent()
                }
            }
        }

        if (extractContent) {
            movableContent()
        }
    }
}