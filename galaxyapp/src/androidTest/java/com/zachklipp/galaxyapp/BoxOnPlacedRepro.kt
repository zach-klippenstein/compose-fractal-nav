package com.zachklipp.galaxyapp

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * See b/228128961.
 */
@RunWith(AndroidJUnit4::class)
class BoxOnPlacedRepro {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun onBoxPlaced_failing() {
        var coordinates: LayoutCoordinates? = null
        rule.setContent {
            Box(Modifier.onPlaced { coordinates = it })
        }
        rule.runOnIdle {
            assertNotNull(coordinates)
        }
    }

    @Test
    fun onBoxPlaced_passing() {
        var coordinates: LayoutCoordinates? = null
        rule.setContent {
            Box(Modifier
                .onPlaced { coordinates = it }
                .layout { m, c ->
                    with(m.measure(c)) {
                        layout(width, height) {
                            place(IntOffset.Zero)
                        }
                    }
                }
            )
        }
        rule.runOnIdle {
            assertNotNull(coordinates)
        }
    }
}