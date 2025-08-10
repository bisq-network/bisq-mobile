package network.bisq.mobile.presentation.ui.components.organisms.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.presentation.ui.components.molecules.JumpToBottomFloatingButton
import network.bisq.mobile.presentation.ui.components.molecules.chat.TextMessageBox
import network.bisq.mobile.presentation.ui.components.molecules.chat.private_messages.ChatRulesWarningMessageBox
import network.bisq.mobile.presentation.ui.components.molecules.chat.trade.ProtocolLogMessageBox
import network.bisq.mobile.presentation.ui.components.molecules.chat.trade.TradePeerLeftMessageBox
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.trade_chat.TradeChatPresenter

@Composable
fun ChatMessageList(
    messages: List<BisqEasyOpenTradeMessageModel>,
    presenter: TradeChatPresenter,
    scrollState: LazyListState,
    avatarMap: Map<String, PlatformImage?> = emptyMap(),
    modifier: Modifier = Modifier,
    onAddReaction: (BisqEasyOpenTradeMessageModel, ReactionEnum) -> Unit = { message: BisqEasyOpenTradeMessageModel, reaction: ReactionEnum -> },
    onRemoveReaction: (BisqEasyOpenTradeMessageModel, BisqEasyOpenTradeMessageReactionVO) -> Unit = { message: BisqEasyOpenTradeMessageModel, reaction: BisqEasyOpenTradeMessageReactionVO -> },
    onReply: (BisqEasyOpenTradeMessageModel) -> Unit = {},
    onCopy: (BisqEasyOpenTradeMessageModel) -> Unit = {},
    onIgnoreUser: (BisqEasyOpenTradeMessageModel) -> Unit = {},
    onReportUser: (BisqEasyOpenTradeMessageModel) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val showChatRulesWarnBox by presenter.showChatRulesWarnBox.collectAsState()

    Box(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
        ) {
            if (showChatRulesWarnBox) {
                ChatRulesWarningMessageBox(presenter)
            }

            LazyColumn(
                reverseLayout = false,
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
            ) {
                items(messages, key = { it.id }) { message ->
                    if (message.chatMessageType == ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE) {
                        ProtocolLogMessageBox(message)
                    } else if (message.chatMessageType == ChatMessageTypeEnum.LEAVE) {
                        TradePeerLeftMessageBox(message)
                    } else {
                        TextMessageBox(
                            message = message,
                            userAvatar = avatarMap.get(message.senderUserProfile.nym),
                            onScrollToMessage = { id ->
                                val index = messages.indexOfFirst { it.id == id }
                                if (index >= 0) {
                                    scope.launch {
                                        scrollState.animateScrollToItem(index, -50)
                                    }
                                }
                            },
                            onAddReaction = { reaction -> onAddReaction(message, reaction) },
                            onRemoveReaction = { reaction -> onRemoveReaction(message, reaction) },
                            onReply = { onReply(message) },
                            onCopy = { onCopy(message) },
                            onIgnoreUser = { onIgnoreUser(message) },
                            onReportUser = { onReportUser(message) },
                        )
                    }
                }
                item { }
            }
        }

        val jumpToBottomButtonEnabled by remember {
            derivedStateOf { scrollState.canScrollForward }
        }

        JumpToBottomFloatingButton(
            enabled = jumpToBottomButtonEnabled,
            onClicked = { scope.launch { scrollState.animateScrollToItem(Int.MAX_VALUE) } },
            jumpOffset = 48,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}