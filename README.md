# compose-fractal-nav

A proof-of-concept of a fractal/recursive navigation system.

Instead of defining a bunch of top-level routes with navigation moving laterally between them, you
define your navigation recursively. You can think of it like showing your entire composable UI at
once, where certain parts can be zoomed-into. It's hard to explain and I'm writing this README after
3 straight days of hacking on my vacation, so easier to just show you. This is the app in this repo:

https://user-images.githubusercontent.com/101754/161682976-66d138a0-b8b9-4fde-a3f6-059294a5225b.mp4

## API

Like most navigation libraries, this one starts with a wrapper composable that defines the navigable
area. It's called `FractalNavHost`. It provides a `FractalNavScope` to its content composable.
Inside that block, you can define "zoomable" children with the `FractalNavChild` composable. Each
child is identified with a string key, and you can zoom into a child by calling `zoomToChild()`.

The content block of a `FractalNavChild` gets access to a few properties that describe whether or
not it's zoomed in, how far it's zoomed, and a function that zooms it back out to its parent.

Children can also define their own children, recursing as deep as you like. The library will only
compose what's necessary to show the active child. Everything between the `FractalNavHost` and
the `FractalNavChild` will be removed from the composition – even when multiple children are nested.
When a child zooms back out, its parent content is composed again, with any state
from `rememberSaveable`s restored.

For more details, take a look
at [`FractalNavScope` and `FractalNavChildScope`](galaxyapp/src/main/java/com/zachklipp/fractalnav/FractalNavScope.kt).

## API sample

Let's build this simple app:

https://user-images.githubusercontent.com/101754/161687429-9aabfd7d-c722-48c3-a979-f25690a1684a.mp4

Here's the code:

```kotlin
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
```

And here's the `Link` function:

```kotlin
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
```
