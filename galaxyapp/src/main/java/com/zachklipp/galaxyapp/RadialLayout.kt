package com.zachklipp.galaxyapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.round
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val HalfPI = PI.toFloat() / 180f

object RadialLayoutScope {
    /**
     * Specifies the offset in pixels from the center of a [RadialLayout] to place the center of
     * this element.
     */
    fun Modifier.centerOffsetPercent(radiusPercent: () -> Float): Modifier = radialParentData {
        it.centerOffsetPercent = radiusPercent
    }

    /**
     * Specifies the angle to place this element around the center of a [RadialLayout].
     */
    fun Modifier.angleDegrees(angle: () -> Float): Modifier = radialParentData {
        it.angleDegrees = angle
    }

    /**
     * Specifies the weight to measure this element relative to the other children of this
     * [RadialLayout].
     */
    fun Modifier.weight(weight: () -> Float): Modifier = radialParentData {
        it.weight = weight
    }

    /**
     * Register a callback to receive the element's distance from the center when measured.
     */
    fun Modifier.onRadiusMeasured(block: (Float) -> Unit): Modifier = radialParentData { it ->
        val previousCallback = it.onRadiusMeasured
        it.onRadiusMeasured = { radius ->
            previousCallback?.invoke(radius)
            block(radius)
        }
    }

    private fun Modifier.radialParentData(block: (RadialLayoutParentData) -> Unit) =
        this.then(object : ParentDataModifier {
            override fun Density.modifyParentData(parentData: Any?) =
                ((parentData as? RadialLayoutParentData) ?: RadialLayoutParentData()).also {
                    block(it)
                }
        })
}

private data class RadialLayoutParentData(
    var centerOffsetPercent: () -> Float = { 0f },
    var angleDegrees: () -> Float = { 0f },
    var weight: (() -> Float)? = null,
    var onRadiusMeasured: ((Float) -> Unit)? = null
)

/**
 * Lays children out in a circle around the center of the layout.
 */
@Composable
fun RadialLayout(
    modifier: Modifier = Modifier,
    content: @Composable RadialLayoutScope.() -> Unit
) {
    Layout(
        modifier = modifier,
        content = {
            RadialLayoutScope.content()
        },
    ) { measurables, constraints ->
        val minDimension = minOf(constraints.maxWidth, constraints.maxHeight)
        val maxRadius = minDimension / 2f
        val center = Offset(maxRadius, maxRadius)
        var totalWeight = 0f

        // Only consider nodes with parent data.
        val (weighted, unweighted) = measurables
            .mapNotNull {
                val layoutData =
                    (it.parentData as? RadialLayoutParentData) ?: return@mapNotNull null
                totalWeight += layoutData.weight?.invoke() ?: 0f
                Pair(it, layoutData)
            }
            .partition { (_, data) -> data.weight.let { it != null && !it.invoke().isNaN() } }

        // TODO measure unweighted placeables.
        val weightedPlaceables = weighted.map { (measurable, data) ->
            val size: Int = if (totalWeight == 0f) {
                minDimension
            } else {
                (minDimension * (data.weight!!() / totalWeight)).roundToInt()
            }
            val weightedConstraints = Constraints(
                maxWidth = size,
                maxHeight = size
            )
            Pair(measurable.measure(weightedConstraints), data)
        }

        layout(minDimension, minDimension) {
            weightedPlaceables.forEachIndexed { i, (placeable, data) ->
                val offset = data.centerOffsetPercent()
                val centerRadius = maxRadius * offset
                data.onRadiusMeasured?.invoke(centerRadius)
                val angle = data.angleDegrees() * HalfPI
                val centerPosition = center + Offset(
                    x = centerRadius * cos(angle),
                    y = centerRadius * sin(angle)
                )
                val topLeftPosition =
                    centerPosition - Offset(placeable.width / 2f, placeable.height / 2f)
                placeable.place(topLeftPosition.round())
            }
        }
    }
}