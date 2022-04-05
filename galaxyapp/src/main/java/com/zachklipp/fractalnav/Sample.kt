package com.zachklipp.fractalnav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun FractalNavSample() {
    MaterialTheme(colors = darkColors()) {
        Surface {
// The host should wrap the root of your app.
            FractalNavHost(Modifier.fillMaxSize()) {
                Row(Modifier.wrapContentSize()) {
                    Text("Click ")
                    Link("here") {
                        Text(
                            "42",
                            // Scale the text in when clicked.
                            modifier = Modifier.scaleByZoomFactor(),
                            style = MaterialTheme.typography.h1,
                            maxLines = 1
                        )
                    }
                    Text(" to learn more.")
                }
            }
        }
    }
}

@Composable
fun FractalNavScope.Link(
    text: String,
    content: @Composable FractalNavChildScope.() -> Unit
) {
    // This creates some content that can be zoomed into.
    FractalNavChild(
        // It's identified by a string key…
        key = "link",
        modifier = Modifier.clickable {
            // …which can be used to expand its content. This will animate
            // the content block below to take up the full screen and also
            // zoom the parent content, that called this composable, out of
            // view.
            zoomToChild("link")
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, Modifier.graphicsLayer {
                // The zoomFactor property is available inside the FractalNavChild
                // block. It starts at 0, then when zoomToChild is called it will
                // be animated up to 1. In this case, we want this text to start
                // at the full size and shrink when zoomed in.
                alpha = 1f - zoomFactor
            })

            // The isActive flag is also provided inside the FractalNavChild
            // content block, and means `zoomFactor > 0` – but is backed by
            // a derivedStateOf so it won't invalidate more than once during
            // the zoom animation.
            if (isActive) {
                content()
                BackHandler {
                    // This will animate zoomFactor back down to 0.
                    zoomToParent()
                }
            }
        }
    }
}