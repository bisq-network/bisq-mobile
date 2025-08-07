package network.bisq.mobile.presentation.ui.helpers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.HorizontalOrVertical
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastRoundToInt


// some of the following functions are taken directly from androidx.compose.foundation.layout.Arrangement,
// we had to copy them because we couldn't create `Arrangement.spaceBetweenWithMin` without them.

private inline fun IntArray.forEachIndexed(reversed: Boolean, action: (Int, Int) -> Unit) {
    if (!reversed) {
        forEachIndexed(action)
    } else {
        for (i in (size - 1) downTo 0) {
            action(i, get(i))
        }
    }
}

private fun placeSpaceBetween(
    totalSize: Int,
    size: IntArray,
    outPosition: IntArray,
    reverseInput: Boolean
) {
    if (size.isEmpty()) return

    val consumedSize = size.fold(0) { a, b -> a + b }
    val noOfGaps = maxOf(size.lastIndex, 1)
    val gapSize = (totalSize - consumedSize).toFloat() / noOfGaps

    var current = 0f
    if (reverseInput && size.size == 1) {
        // If the layout direction is right-to-left and there is only one gap,
        // we start current with the gap size. That forces the single item to be right-aligned.
        current = gapSize
    }
    size.forEachIndexed (reverseInput) { index, it ->
        outPosition[index] = current.fastRoundToInt()
        current += it.toFloat() + gapSize
    }
}

@Stable
fun Arrangement.spaceBetweenWithMin(minSpace: Dp): Arrangement.HorizontalOrVertical {
    return object : HorizontalOrVertical {
        override val spacing = minSpace

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            layoutDirection: LayoutDirection,
            outPositions: IntArray
        ) = if (layoutDirection == LayoutDirection.Ltr) {
            placeSpaceBetween(totalSize, sizes, outPositions, reverseInput = false)
        } else {
            placeSpaceBetween(totalSize, sizes, outPositions, reverseInput = true)
        }

        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray
        ) = placeSpaceBetween(totalSize, sizes, outPositions, reverseInput = false)

        override fun toString() = "Arrangement#SpaceBetweenWithMin"
    }
}