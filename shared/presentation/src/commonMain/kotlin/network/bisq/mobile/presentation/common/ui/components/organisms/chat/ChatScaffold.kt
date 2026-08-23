package network.bisq.mobile.presentation.common.ui.components.organisms.chat

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import network.bisq.mobile.data.replicated.chat.priv.PrivateChatMessage
import network.bisq.mobile.presentation.common.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.ChatInputBottomBar
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.ChatInputField
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING

/**
 * Scaffold for chat screens, owning the layout contract between the message content and the
 * [ChatInputField] bottom bar: the input carries its own margins via [ChatInputBottomBar], so the
 * content column drops its bottom padding — otherwise that padding would land as a gap between
 * the message list and the input. Keeping both halves here means chat screens cannot copy one
 * without the other.
 */
@Composable
fun ChatScaffold(
    onMessageSend: (String) -> Unit,
    topBar: @Composable (() -> Unit)? = null,
    quotedMessage: PrivateChatMessage<*>? = null,
    placeholder: String = EMPTY_STRING,
    resetScroll: () -> Unit = {},
    onCloseReply: () -> Unit = {},
    sendEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    BisqStaticScaffold(
        padding =
            PaddingValues(
                top = BisqUIConstants.ScreenPadding,
                start = BisqUIConstants.ScreenPadding,
                end = BisqUIConstants.ScreenPadding,
                bottom = BisqUIConstants.Zero,
            ),
        topBar = topBar,
        bottomBar = {
            ChatInputBottomBar(
                onMessageSend = onMessageSend,
                quotedMessage = quotedMessage,
                placeholder = placeholder,
                resetScroll = resetScroll,
                onCloseReply = onCloseReply,
                sendEnabled = sendEnabled,
            )
        },
        content = content,
    )
}
