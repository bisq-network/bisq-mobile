package network.bisq.mobile.presentation.ui.components.organisms.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.molecules.JumpToBottomFloatingButton
import network.bisq.mobile.presentation.ui.components.molecules.chat.ChatTextMessageBox
import network.bisq.mobile.presentation.ui.components.molecules.chat.private_messages.ChatRulesWarningMessageBox
import network.bisq.mobile.presentation.ui.components.molecules.chat.trade.ProtocolLogMessageBox
import network.bisq.mobile.presentation.ui.components.molecules.chat.trade.TradePeerLeftMessageBox
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ChatMessageList(
    messages: List<BisqEasyOpenTradeMessageModel>,
    ignoredUserIds: Set<String>,
    showChatRulesWarnBox: Boolean,
    readCount: Int,
    userProfileIconProvider: () -> suspend (UserProfileVO) -> PlatformImage,
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
    onResendMessage: (String) -> Unit,
    userNameProvider: suspend (String) -> String,
) {
    val scope = rememberCoroutineScope()
    var jumpToBottomVisible by remember { mutableStateOf(false) }
    val unreadCount = remember(messages, readCount) {
        (messages.size - readCount).coerceAtLeast(0)
    }
    val scrollState = rememberLazyListState(
        initialFirstVisibleItemIndex = unreadCount.coerceIn(0, messages.size)
    )
    val canScrollDown by remember {
        derivedStateOf { scrollState.canScrollBackward }
    }
    val firstVisibleItemIndex by remember {
        derivedStateOf { scrollState.firstVisibleItemIndex }
    }

    var initialReadCount by remember { mutableStateOf(readCount) }

    val unreadMarkerIndex = remember(messages, initialReadCount, canScrollDown) {
        if (canScrollDown) {
            (messages.size - initialReadCount).coerceIn(0, messages.size)
        } else {
            0
        }
    }

    LaunchedEffect(canScrollDown) {
        // effect will be cancelled as canScrollDown changes
        if (canScrollDown) {
            delay(400)
            // 1 is to account for spacer
            if (scrollState.firstVisibleItemIndex > 1) {
                jumpToBottomVisible = true
            }
        } else {
            jumpToBottomVisible = false
        }
    }

    LaunchedEffect(firstVisibleItemIndex, unreadCount) {
        // firstVisibleItemIndex starts from 1 for our messages
        // because we have an extra item for padding at the start of the list
        if (firstVisibleItemIndex == 0) {
            initialReadCount = messages.size
            onUpdateReadCount(messages.size)
        } else if (firstVisibleItemIndex < unreadCount) {
            // what this does is that it will mark messages as read 1 by 1
            // as user scrolls down or new messages arrive
            val newReadCount = readCount + (unreadCount - firstVisibleItemIndex)
            onUpdateReadCount(newReadCount)
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

            val userProfileIconProvider = remember(userProfileIconProvider) { userProfileIconProvider() }

            LazyColumn(
                reverseLayout = true,
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
            ) {
                item { }

                itemsIndexed(
                    items = messages,
                    key = { i, m -> m.id },
                    contentType = { i, m -> m.chatMessageType },
                ) { i, message ->
                    if (unreadMarkerIndex > 0 && i == unreadMarkerIndex) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f).padding(vertical = BisqUIConstants.ScreenPadding),
                                thickness = 2.dp,
                                color = BisqTheme.colors.primary
                            )
                            BisqText.baseRegular(
                                text = "mobile.chat.unreadMessages".i18n(),
                                color = BisqTheme.colors.primary,
                                modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPaddingHalf)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f).padding(vertical = BisqUIConstants.ScreenPadding),
                                thickness = 2.dp,
                                color = BisqTheme.colors.primary
                            )
                        }
                    }
                    when (message.chatMessageType) {
                        ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE -> {
                            ProtocolLogMessageBox(
                                message,
                                Modifier.animateItem(
                                    fadeInSpec = fadeAnimSpec,
                                    fadeOutSpec = fadeAnimSpec,
                                    placementSpec = placementAnimSpec
                                ),
                                onResendMessage = onResendMessage,
                                userNameProvider = userNameProvider,
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
                            ChatTextMessageBox(
                                message = message,
                                userProfileIconProvider = userProfileIconProvider,
                                onScrollToMessage = { id ->
                                    val index = messages.indexOfFirst { it.id == id }
                                    if (index >= 0) {
                                        scope.launch {
                                            // +1 accounts for the spacer at index 0
                                            scrollState.animateScrollToItem(index + 1, -50)
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
                                onResendMessage = onResendMessage,
                                userNameProvider = userNameProvider,
                            )
                        }
                    }
                }
            }
        }

        JumpToBottomFloatingButton(
            visible = jumpToBottomVisible,
            onClicked = {
                scope.launch {
                    if (scrollState.firstVisibleItemIndex == unreadMarkerIndex) {
                        scrollState.animateScrollToItem(0)
                    } else {
                        scrollState.animateScrollToItem(unreadMarkerIndex)
                    }
                }
            },
            jumpOffset = 12,
            badgeCount = unreadCount,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}