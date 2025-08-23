package network.bisq.mobile.presentation.ui.components.organisms.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
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

@Composable
fun ChatMessageList(
    messages: List<BisqEasyOpenTradeMessageModel>,
    ignoredUserIds: Set<String>,
    showChatRulesWarnBox: Boolean,
    readCount: Int,
    avatarMap: Map<String, PlatformImage?> = emptyMap(),
    onAddReaction: (BisqEasyOpenTradeMessageModel, ReactionEnum) -> Unit = { message: BisqEasyOpenTradeMessageModel, reaction: ReactionEnum -> },
    onRemoveReaction: (BisqEasyOpenTradeMessageModel, BisqEasyOpenTradeMessageReactionVO) -> Unit = { message: BisqEasyOpenTradeMessageModel, reaction: BisqEasyOpenTradeMessageReactionVO -> },
    onReply: (BisqEasyOpenTradeMessageModel) -> Unit = {},
    onCopy: (BisqEasyOpenTradeMessageModel) -> Unit = {},
    onIgnoreUser: (String) -> Unit = {},
    onUndoIgnoreUser: (String) -> Unit = {},
    onReportUser: (BisqEasyOpenTradeMessageModel) -> Unit = {},
    onOpenChatRules: () -> Unit = {},
    onDontShowAgainChatRulesWarningBox: () -> Unit = {},
    onUpdateReadCount: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var jumpToBottomVisible by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val canScrollBackward by remember {
        derivedStateOf { scrollState.canScrollBackward }
    }
    val firstVisibleItemIndex by remember {
        derivedStateOf { scrollState.firstVisibleItemIndex }
    }

    val unreadCount = remember(messages, readCount) {
        if (messages.isNotEmpty()) {
            messages.size - readCount
        } else {
            0
        }
    }

    LaunchedEffect(canScrollBackward) {
        // effect will be cancelled as canScrollBackward changes
        if (canScrollBackward) {
            delay(400)
            jumpToBottomVisible = true
        } else {
            jumpToBottomVisible = false
        }
    }

    LaunchedEffect(firstVisibleItemIndex, unreadCount) {
        // firstVisibleItemIndex starts from 1 for our messages
        // because we have an extra item for padding at the start of the list
        if (firstVisibleItemIndex <= unreadCount) {
            // what this does is that it will mark messages as read 1 by 1
            // as user scrolls down or new messages arrive
            onUpdateReadCount(readCount + (unreadCount - firstVisibleItemIndex))
        }
    }

    Box(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
        ) {
            if (showChatRulesWarnBox) {
                ChatRulesWarningMessageBox(
                    onOpenChatRules = onOpenChatRules,
                    onDontShowAgainChatRulesWarningBox = onDontShowAgainChatRulesWarningBox,
                )
            }

            val placementAnimSpec: FiniteAnimationSpec<IntOffset> = tween(
                durationMillis = 100,
                easing = FastOutSlowInEasing
            )

            val fadeAnimSpec: FiniteAnimationSpec<Float> = tween(
                durationMillis = 100,
                easing = FastOutSlowInEasing
            )

            LazyColumn(
                reverseLayout = true,
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
            ) {
                item { }

                items(messages, key = { it.id }) { message ->
                    when (message.chatMessageType) {
                        ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE -> {
                            ProtocolLogMessageBox(
                                message,
                                Modifier.animateItem(
                                    fadeInSpec = fadeAnimSpec,
                                    fadeOutSpec = fadeAnimSpec,
                                    placementSpec = placementAnimSpec
                                )
                            )
                        }

                        ChatMessageTypeEnum.LEAVE -> {
                            TradePeerLeftMessageBox(
                                message,
                                Modifier.animateItem(
                                    fadeInSpec = fadeAnimSpec,
                                    fadeOutSpec = fadeAnimSpec,
                                    placementSpec = placementAnimSpec
                                )
                            )
                        }

                        else -> {
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
                                onRemoveReaction = { reaction ->
                                    onRemoveReaction(
                                        message,
                                        reaction
                                    )
                                },
                                onReply = { onReply(message) },
                                onCopy = { onCopy(message) },
                                onIgnoreUser = { onIgnoreUser(message.senderUserProfileId) },
                                onUndoIgnoreUser = { onUndoIgnoreUser(message.senderUserProfileId) },
                                onReportUser = { onReportUser(message) },
                                isIgnored = ignoredUserIds.contains(message.senderUserProfileId),
                                modifier = Modifier.animateItem(
                                    fadeInSpec = fadeAnimSpec,
                                    fadeOutSpec = fadeAnimSpec,
                                    placementSpec = placementAnimSpec
                                ),
                            )
                        }
                    }
                }
            }
        }

        JumpToBottomFloatingButton(
            visible = jumpToBottomVisible,
            onClicked = { scope.launch { scrollState.animateScrollToItem(0) } },
            badgeCount = unreadCount,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}