package network.bisq.mobile.presentation.common.ui.components.atoms.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp

/**
 * Lays [leading], [separator] and [trailing] out on one line while they fit, like a vertically
 * centred `Row`; otherwise stacks [trailing] below [leading] and leaves the separator out.
 *
 * They fit when [trailing] still gets the width it needs beside the single-line width of the
 * others: its natural width capped at [trailingMinWidth], or, with no minimum, its longest
 * unbreakable word, so it never has to be split inside one. The decision comes from intrinsic
 * widths alone, so the same widths always give the same layout.
 *
 * A plain [Layout] rather than a subcomposition, so callers can still be measured with intrinsics;
 * its own intrinsic heights follow the same decision as its measurement.
 */
@Composable
fun InlineOrStacked(
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    separator: @Composable () -> Unit = {},
    inlineSpacing: Dp = 0.dp,
    stackedSpacing: Dp = 0.dp,
    trailingMinWidth: Dp? = null,
    stackedAlignment: Alignment.Horizontal = Alignment.Start,
) {
    val measurePolicy =
        remember(inlineSpacing, stackedSpacing, trailingMinWidth, stackedAlignment) {
            InlineOrStackedMeasurePolicy(inlineSpacing, stackedSpacing, trailingMinWidth, stackedAlignment)
        }
    Layout(
        contents = listOf(leading, separator, trailing),
        modifier = modifier,
        measurePolicy = measurePolicy,
    )
}

private class InlineOrStackedMeasurePolicy(
    private val inlineSpacing: Dp,
    private val stackedSpacing: Dp,
    private val trailingMinWidth: Dp?,
    private val stackedAlignment: Alignment.Horizontal,
) : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val (leading, separator, trailing) = measurables.map { it.firstOrNull() }
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Constraints.Infinity

        if (fitsInline(leading, separator, trailing, maxWidth)) {
            val inlineSpacingPx = inlineSpacing.roundToPx()
            // Measured in order with the width left over, exactly as a Row measures unweighted children.
            val items = listOfNotNull(leading, separator, trailing)
            var remaining = maxWidth
            val placeables =
                items.mapIndexed { index, measurable ->
                    val spacing = if (index == 0) 0 else inlineSpacingPx
                    val available =
                        if (remaining == Constraints.Infinity) Constraints.Infinity else (remaining - spacing).coerceAtLeast(0)
                    measurable.measure(loose.copy(maxWidth = available)).also {
                        if (remaining != Constraints.Infinity) remaining -= spacing + it.width
                    }
                }
            val width = placeables.sumOf { it.width } + inlineSpacingPx * (placeables.size - 1).coerceAtLeast(0)
            // Centred in the final height, so a minimum height from the caller centres the line once.
            val height = constraints.constrainHeight(placeables.maxOfOrNull { it.height } ?: 0)
            return layout(constraints.constrainWidth(width), height) {
                var x = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(x, Alignment.CenterVertically.align(placeable.height, height))
                    x += placeable.width + inlineSpacingPx
                }
            }
        }

        val stackedSpacingPx = stackedSpacing.roundToPx()
        val placeables = listOfNotNull(leading, trailing).map { it.measure(loose) }
        val width = placeables.maxOfOrNull { it.width } ?: 0
        val height = placeables.sumOf { it.height } + stackedSpacingPx * (placeables.size - 1).coerceAtLeast(0)
        return layout(constraints.constrainWidth(width), constraints.constrainHeight(height)) {
            var y = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(stackedAlignment.align(placeable.width, width, layoutDirection), y)
                y += placeable.height + stackedSpacingPx
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int,
    ): Int = intrinsicHeight(measurables, width) { item, itemWidth -> item.minIntrinsicHeight(itemWidth) }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int,
    ): Int = intrinsicHeight(measurables, width) { item, itemWidth -> item.maxIntrinsicHeight(itemWidth) }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int,
    ): Int {
        // The narrowest it can be is stacked, each part at its own narrowest.
        val (leading, _, trailing) = measurables.map { it.firstOrNull() }
        return listOfNotNull(leading, trailing).maxOfOrNull { it.minIntrinsicWidth(height) } ?: 0
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int,
    ): Int {
        // The widest it needs is everything on one line.
        val items = measurables.mapNotNull { it.firstOrNull() }
        return items.sumOf { it.maxIntrinsicWidth(height) } + inlineSpacing.roundToPx() * (items.size - 1).coerceAtLeast(0)
    }

    private fun IntrinsicMeasureScope.intrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int,
        heightOf: (IntrinsicMeasurable, Int) -> Int,
    ): Int {
        val (leading, separator, trailing) = measurables.map { it.firstOrNull() }
        if (fitsInline(leading, separator, trailing, width)) {
            val inlineSpacingPx = inlineSpacing.roundToPx()
            var remaining = width
            return listOfNotNull(leading, separator, trailing)
                .mapIndexed { index, item ->
                    val spacing = if (index == 0) 0 else inlineSpacingPx
                    val available =
                        if (remaining == Constraints.Infinity) Constraints.Infinity else (remaining - spacing).coerceAtLeast(0)
                    val itemWidth = minOf(item.maxIntrinsicWidth(Constraints.Infinity), available)
                    if (remaining != Constraints.Infinity) remaining -= spacing + itemWidth
                    heightOf(item, itemWidth)
                }.maxOrNull() ?: 0
        }
        val stacked = listOfNotNull(leading, trailing)
        return stacked.sumOf { heightOf(it, width) } + stackedSpacing.roundToPx() * (stacked.size - 1).coerceAtLeast(0)
    }

    private fun Density.fitsInline(
        leading: IntrinsicMeasurable?,
        separator: IntrinsicMeasurable?,
        trailing: IntrinsicMeasurable?,
        maxWidth: Int,
    ): Boolean {
        if (maxWidth == Constraints.Infinity) return true
        val leadingWidth = leading?.maxIntrinsicWidth(Constraints.Infinity) ?: 0
        val separatorWidth = separator?.maxIntrinsicWidth(Constraints.Infinity) ?: 0
        val trailingNeeded =
            trailing?.let { item ->
                trailingMinWidth?.let { minWidth ->
                    minOf(item.maxIntrinsicWidth(Constraints.Infinity), minWidth.roundToPx())
                } ?: item.minIntrinsicWidth(Constraints.Infinity)
            } ?: 0
        val itemCount = listOfNotNull(leading, separator, trailing).size
        val spacing = inlineSpacing.roundToPx() * (itemCount - 1).coerceAtLeast(0)
        return leadingWidth + separatorWidth + trailingNeeded + spacing <= maxWidth
    }
}
