package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.composeModels.ChatMessage
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun QuoteMessageBubble(
    message: ChatMessage,
    content: @Composable () -> Unit
) {
    val sideBorderColor = BisqTheme.colors.grey2
    Column(
        modifier = Modifier
            .padding(
                top = BisqUIConstants.ScreenPaddingHalf,
                start = BisqUIConstants.ScreenPaddingHalf,
                end = BisqUIConstants.ScreenPaddingHalf
            )
            .clip(shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf))
            .fillMaxWidth()
    ) {
        // TODO: On click, scroll up to the quoted message
        Column(
            modifier = Modifier
                .background(BisqTheme.colors.grey5)
                .clip(shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf))
                .drawBehind {
                    drawLine(
                        color = sideBorderColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 10.dp.toPx()
                    )
                }
                .padding(
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                    horizontal = BisqUIConstants.ScreenPadding
                ),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)
        ) {
            BisqText.baseMedium(text = message.chatMessageReplyOf?.author ?: "")
            BisqText.baseMedium(text = message.chatMessageReplyOf?.content ?: "") // TODO: Trim this to max 2 lines
        }

        content()
    }
}