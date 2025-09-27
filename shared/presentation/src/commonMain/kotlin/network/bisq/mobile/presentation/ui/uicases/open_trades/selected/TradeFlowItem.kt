package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_step_active_1
import bisqapps.shared.presentation.generated.resources.trade_step_active_2
import bisqapps.shared.presentation.generated.resources.trade_step_active_3
import bisqapps.shared.presentation.generated.resources.trade_step_inactive_1
import bisqapps.shared.presentation.generated.resources.trade_step_inactive_2
import bisqapps.shared.presentation.generated.resources.trade_step_inactive_3
import bisqapps.shared.presentation.generated.resources.trade_step_inactive_4
import bisqapps.shared.presentation.generated.resources.trade_step_visited
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
    val textColor = if (isActive) BisqTheme.colors.white else BisqTheme.colors.mid_grey20
    val lineColor = BisqTheme.colors.dark_grey50
    val spacerBetweenNodes: Dp = if (isLastIndex) 5.dp else 20.dp
    val stepNumber = 1 + index

    Box(
        modifier = Modifier.wrapContentSize().drawBehind {
            drawLine(
                color = lineColor,
                start = Offset(x = 12.dp.toPx(), y = 12.dp.toPx() * 2),
                end = Offset(x = 12.dp.toPx(), y = this.size.height),
                strokeWidth = 1.dp.toPx()
            )
        },
    ) {
        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.Center
        ) {

            val modifier = Modifier.size(24.dp)
            if (isVisited) {
                Image(
                    painterResource(Res.drawable.trade_step_visited), "trade step visited",
                    modifier = modifier
                )

            } else if (isActive) {
                when (stepNumber) {
                    1 -> {
                        Image(
                            painterResource(Res.drawable.trade_step_active_1), "trade step 1",
                            modifier = modifier
                        )
                    }

                    2 -> {
                        Image(
                            painterResource(Res.drawable.trade_step_active_2), "trade step 2",
                            modifier = modifier
                        )
                    }

                    3 -> {
                        Image(
                            painterResource(Res.drawable.trade_step_active_3), "trade step 3",
                            modifier = modifier
                        )
                    }
                }
                // Step 4 active is never shown as we move to the completed screen

            } else {
                when (stepNumber) {
                    1 -> {
                        Image(
                            painterResource(Res.drawable.trade_step_inactive_1), "trade step inactive 1",
                            modifier = modifier
                        )
                    }

                    2 -> {
                        Image(
                            painterResource(Res.drawable.trade_step_inactive_2), "trade step inactive 2",
                            modifier = modifier
                        )
                    }

                    3 -> {
                        Image(
                            painterResource(Res.drawable.trade_step_inactive_3), "trade step inactive 3",
                            modifier = modifier
                        )
                    }

                    4 -> {
                        Image(
                            painterResource(Res.drawable.trade_step_inactive_4), "trade step inactive 4",
                            modifier = modifier
                        )
                    }
                }
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
