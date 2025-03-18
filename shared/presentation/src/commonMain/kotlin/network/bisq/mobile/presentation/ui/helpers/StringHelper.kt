package network.bisq.mobile.presentation.ui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.components.atoms.FontSize

object StringHelper {
    @Composable
    fun <T> calculateTotalWidthOfStrings(strings: List<T>): Dp {
        val textMeasurer: TextMeasurer = rememberTextMeasurer()
        val textStyle = TextStyle(fontSize = FontSize.SMALL.size)
        val maxWidthOfText = strings.map {
            textMeasurer.measure(
                it.toString(),
                style = textStyle,
                maxLines = 1,
                constraints = Constraints(maxWidth = Int.MAX_VALUE)
            ).size.width
        }.maxOrNull() ?: 0

        // TODO: Magic number!!! Nothing else really fixes this issue!
        return with(LocalDensity.current) { (maxWidthOfText * 1.5f).toDp() }
    }

    @Composable
    fun <T> calculateMaxHeightOfStrings(strings: List<T>, maxWidth: Dp): Dp {
        val textMeasurer: TextMeasurer = rememberTextMeasurer()
        val textStyle = TextStyle(fontSize = FontSize.SMALL.size)

        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }

        val maxHeight = strings.map {
            textMeasurer.measure(
                it.toString(),
                style = textStyle,
                maxLines = 1,
                constraints = Constraints(maxWidth = maxWidthPx.toInt())
            ).size.height
        }.maxOrNull() ?: 0

        return with(LocalDensity.current) { maxHeight.toDp() }
    }

}