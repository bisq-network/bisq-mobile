package network.bisq.mobile.presentation.ui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

object StringHelper {
    @Composable
    fun <T> calculateTotalWidthOfStrings(strings: List<T>): Dp {
        val textMeasurer: TextMeasurer = rememberTextMeasurer()
        val textStyle =
            TextStyle(fontSize = 14.sp)
        val maxWidthOfText: List<Int> = strings.map {
            textMeasurer.measure(it.toString(), style = textStyle).size.width
        }

        return with(LocalDensity.current) { maxWidthOfText.max().toDp() }
    }

    @Composable
    fun <T> calculateMaxHeightOfStrings(strings: List<T>, maxWidth: Dp): Dp {
        val textMeasurer: TextMeasurer = rememberTextMeasurer()
        val textStyle = TextStyle(fontSize = 14.sp)

        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }

        val maxHeight = strings.map {
            textMeasurer.measure(
                it.toString(),
                style = textStyle,
                constraints = Constraints(maxWidth = maxWidthPx.toInt())
            ).size.height
        }.maxOrNull() ?: 0

        return with(LocalDensity.current) { maxHeight.toDp() }
    }

}