package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun QuoteMessageBubble(
    message: BisqEasyOpenTradeMessageModel,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val sideBorderColor = BisqTheme.colors.mid_grey20
    val isMyMessage = message.isMyMessage
    val bgColor = if (isMyMessage) BisqTheme.colors.dark_grey50 else BisqTheme.colors.dark_grey20

    Column(
        modifier = Modifier
            .padding(
                top = BisqUIConstants.ScreenPaddingHalf,
                start = BisqUIConstants.ScreenPaddingHalf,
                end = BisqUIConstants.ScreenPaddingHalf
            )
            .clip(shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf))
    ) {
        Column(
            modifier = Modifier
                .background(bgColor)
                .clickable(
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                )
                .clip(shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf))
                .drawBehind {
                    drawLine(
                        color = sideBorderColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 4.dp.toPx()
                    )
                }
                .padding(
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                    horizontal = BisqUIConstants.ScreenPadding
                ),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)
        ) {
            BisqText.baseMedium(message.citationAuthorUserName ?: "", color = BisqTheme.colors.mid_grey20)
            BisqText.baseRegular(message.citationString, color = BisqTheme.colors.mid_grey10) // TODO: Trim this to max 2 lines
        }

        content()
    }
}