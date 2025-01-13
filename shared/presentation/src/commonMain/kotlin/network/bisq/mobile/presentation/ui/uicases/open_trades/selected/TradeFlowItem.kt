package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_check_white
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun TradeFlowItem(
    index: Int = 0,
    isVisited: Boolean = false,
    isActive: Boolean = false,
    isLastIndex: Boolean = false,
    contentStartOffset: Dp = 32.dp,
    content: @Composable BoxScope.(modifier: Modifier) -> Unit
) {
    val textColor = if (isActive) BisqTheme.colors.light1 else BisqTheme.colors.grey2
    val lineColor = BisqTheme.colors.dark5
    val greyCircleColor = BisqTheme.colors.dark5
    val spacerBetweenNodes: Dp = if (isLastIndex) 5.dp else 20.dp
    val text = (1 + index).toString()

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
                if (isVisited) {
                    drawCircle(
                        color = primary,
                        radius = 12.dp.toPx(),
                        center = Offset(12.dp.toPx(), 12.dp.toPx()),
                    )
                } else if (isActive) {
                    drawCircle(
                        color = primary,
                        radius = 12.dp.toPx(),
                        center = Offset(12.dp.toPx(), 12.dp.toPx()),
                    )
                    drawCircle(
                        color = greyCircleColor,
                        radius = 11.dp.toPx(),
                        center = Offset(12.dp.toPx(), 12.dp.toPx()),
                    )
                    drawCircle(
                        color = primary,
                        radius = 10.dp.toPx(),
                        center = Offset(12.dp.toPx(), 12.dp.toPx()),
                    )
                } else {
                    drawCircle(
                        color = greyCircleColor,
                        radius = 12.dp.toPx(),
                        center = Offset(12.dp.toPx(), 12.dp.toPx()),
                    )
                }

            }) {

            if (isVisited) {
                Image(
                    painterResource(Res.drawable.trade_check_white), "",
                    modifier = Modifier.height(18.dp).width(18.dp).padding(start = 6.dp, top = 6.dp)
                )

            } else if (isActive) {
                BisqText.baseBold(
                    textAlign = TextAlign.Center,
                    text = text,
                    color = textColor,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )

            } else {
                BisqText.baseRegular(
                    textAlign = TextAlign.Center,
                    text = text,
                    color = textColor,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }

        }
        content(
            Modifier.padding(
                start = contentStartOffset,
                bottom = spacerBetweenNodes
            )
        )
    }
}
