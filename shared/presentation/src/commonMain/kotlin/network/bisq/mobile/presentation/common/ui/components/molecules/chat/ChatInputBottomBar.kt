package network.bisq.mobile.presentation.common.ui.components.molecules.chat

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.data.replicated.chat.priv.PrivateChatMessage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING

/**
 * Standard bottom-bar placement for [ChatInputField]. A scaffold's bottomBar slot sits outside
 * its content padding, so the horizontal and bottom screen insets are restored here, once, for
 * every chat screen instead of at each call site.
 */
@Composable
fun ChatInputBottomBar(
    onMessageSend: (String) -> Unit,
    modifier: Modifier = Modifier,
    quotedMessage: PrivateChatMessage<*>? = null,
    placeholder: String = EMPTY_STRING,
    resetScroll: () -> Unit = {},
    onCloseReply: () -> Unit = {},
    sendEnabled: Boolean = true,
) {
    ChatInputField(
        modifier =
            modifier.padding(
                start = BisqUIConstants.ScreenPadding,
                end = BisqUIConstants.ScreenPadding,
                bottom = BisqUIConstants.ScreenPadding,
            ),
        quotedMessage = quotedMessage,
        placeholder = placeholder,
        onMessageSend = onMessageSend,
        resetScroll = resetScroll,
        onCloseReply = onCloseReply,
        sendEnabled = sendEnabled,
    )
}

@Preview
@Composable
private fun ChatInputBottomBarPreview() {
    BisqTheme.Preview {
        ChatInputBottomBar(
            onMessageSend = {},
            placeholder = "chat.message.input.prompt".i18n(),
        )
    }
}
