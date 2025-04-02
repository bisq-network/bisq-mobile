package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun StepperSection(
    stepNumber: Int = 0,
    isActive: Boolean = false,
    isLastIndex: Boolean = false,
    contentStartOffset: Dp = 32.dp,
    spacerBetweenNodes: Dp = 32.dp,
    content: @Composable BoxScope.(modifier: Modifier) -> Unit
) {
    val lineColor = BisqTheme.colors.dark_grey50
    Box(
        modifier = Modifier.wrapContentSize().drawBehind {
            drawLine(
                color = lineColor,
                start = Offset(x = 12.dp.toPx(), y = 12.dp.toPx() * 2),
                end = Offset(x = 12.dp.toPx(), y = this.size.height),
                strokeWidth = 1.dp.toPx()
            )

        }
    ) {
        val primary = BisqTheme.colors.primary
        Box(
            modifier = Modifier.wrapContentSize().drawBehind {
                if (isActive) {
                    drawCircle(
                        color = lineColor,
                        radius = 12.dp.toPx(),
                        center = Offset(12.dp.toPx(), 12.dp.toPx()),
                    )
                    drawCircle(
                        color = primary,
                        radius = 8.dp.toPx(),
                        center = Offset(12.dp.toPx(), 12.dp.toPx()),
                    )
                    drawCircle(
                        color = primary,
                        radius = 12.dp.toPx(),
                        center = Offset(12.dp.toPx(), 12.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                } else {
                    drawCircle(
                        color = lineColor,
                        radius = 12.dp.toPx(),
                        center = Offset(12.dp.toPx(), 12.dp.toPx()),
                    )
                }

            }) {

            BisqText.xsmallRegular(
                textAlign = TextAlign.Center,
                text = stepNumber.toString(),
                modifier = Modifier.padding(start = 8.dp).offset(x = 1.dp, y = (-4).dp)
            )
        }
        content(
            Modifier
                .padding(
                    start = contentStartOffset,
                    bottom = if (isLastIndex) {
                        0.dp
                    } else {
                        spacerBetweenNodes
                    }
                )
        )
    }
}
