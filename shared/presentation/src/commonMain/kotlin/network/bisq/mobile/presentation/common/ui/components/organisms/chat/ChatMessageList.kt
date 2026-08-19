// TODO: remove and fix the issue
@file:Suppress("ktlint:compose:lambda-param-in-effect")

package network.bisq.mobile.presentation.common.ui.components.organisms.chat

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.priv.PrivateChatMessage
import network.bisq.mobile.data.replicated.chat.reactions.ChatMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.chat.two_party.createMockTwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.molecules.JumpToBottomFloatingButton
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.ChatTextMessageBox
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.private_messages.ChatRulesWarningMessageBox
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.private_messages.PrivateChatPeerLeftMessageBox
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.trade.ProtocolLogMessageBox
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@Composable
fun <M : PrivateChatMessage<R>, R : ChatMessageReaction> ChatMessageList(
    messages: List<M>,
    ignoredUserIds: Set<String>,
    showChatRulesWarnBox: Boolean,
    readCount: Int,
    userProfileIconProvider: () -> suspend (UserProfileVO) -> PlatformImage,
    onResendMessage: (String) -> Unit,
    userNameProvider: suspend (String) -> String,
    onPeerProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAddReaction: (M, ReactionEnum) -> Unit = { _, _ -> },
    onRemoveReaction: (M, R) -> Unit = { _, _ -> },
    onReply: (M) -> Unit = {},
    onCopy: (M) -> Unit = {},
    onIgnoreUser: (String) -> Unit = {},
    onUndoIgnoreUser: (String) -> Unit = {},
    onReportUser: (M) -> Unit = {},
    onOpenChatRules: () -> Unit = {},
    onDontShowAgainChatRulesWarningBox: () -> Unit = {},
    onUpdateReadCount: (Int) -> Unit = {},
    /**
     * Renders a [ChatMessageTypeEnum.LEAVE] message. Required rather than defaulted to the trade
     * wording: this component is generic now, and a default would let a non-trade caller compile
     * while rendering "has left the trade" — wrong copy that only surfaces on a rare event.
     */
    leaveMessageContent: @Composable (M, Modifier) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var jumpToBottomVisible by remember { mutableStateOf(false) }
    val unreadCount =
        remember(messages, readCount) {
            (messages.size - readCount).coerceAtLeast(0)
        }
    val scrollState =
        rememberLazyListState(
            initialFirstVisibleItemIndex = unreadCount.coerceIn(0, messages.size),
        )
    val canScrollDown by remember {
        derivedStateOf { scrollState.canScrollBackward }
    }
    val firstVisibleItemIndex by remember {
        derivedStateOf { scrollState.firstVisibleItemIndex }
    }

    var initialReadCount by remember { mutableIntStateOf(readCount) }

    val unreadMarkerIndex =
        remember(messages, initialReadCount, canScrollDown) {
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
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
        ) {
            if (showChatRulesWarnBox) {
                ChatRulesWarningMessageBox(
                    onOpenChatRules = onOpenChatRules,
                    onDontShowAgainChatRulesWarningBox = onDontShowAgainChatRulesWarningBox,
                )
            }

            val placementAnimSpec: FiniteAnimationSpec<IntOffset> =
                tween(
                    durationMillis = 100,
                    easing = FastOutSlowInEasing,
                )

            val fadeAnimSpec: FiniteAnimationSpec<Float> =
                tween(
                    durationMillis = 100,
                    easing = FastOutSlowInEasing,
                )

            val userProfileIconProvider = remember(userProfileIconProvider) { userProfileIconProvider() }

            LazyColumn(
                reverseLayout = true,
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
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
                                color = BisqTheme.colors.primary,
                            )
                            BisqText.BaseRegular(
                                text = "mobile.chat.unreadMessages".i18n(),
                                color = BisqTheme.colors.primary,
                                modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPaddingHalf),
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f).padding(vertical = BisqUIConstants.ScreenPadding),
                                thickness = 2.dp,
                                color = BisqTheme.colors.primary,
                            )
                        }
                    }
                    when (message.chatMessageType) {
                        ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE -> {
                            ProtocolLogMessageBox(
                                message,
                                modifier =
                                    Modifier.animateItem(
                                        fadeInSpec = fadeAnimSpec,
                                        fadeOutSpec = fadeAnimSpec,
                                        placementSpec = placementAnimSpec,
                                    ),
                                onResendMessage = onResendMessage,
                                userNameProvider = userNameProvider,
                            )
                        }

                        ChatMessageTypeEnum.LEAVE -> {
                            leaveMessageContent(
                                message,
                                Modifier.animateItem(
                                    fadeInSpec = fadeAnimSpec,
                                    fadeOutSpec = fadeAnimSpec,
                                    placementSpec = placementAnimSpec,
                                ),
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
                                        reaction,
                                    )
                                },
                                onReply = { onReply(message) },
                                onCopy = { onCopy(message) },
                                onIgnoreUser = { onIgnoreUser(message.senderUserProfileId) },
                                onUndoIgnoreUser = { onUndoIgnoreUser(message.senderUserProfileId) },
                                onReportUser = { onReportUser(message) },
                                onPeerProfileClick = { onPeerProfileClick(message.senderUserProfileId) },
                                isIgnored = ignoredUserIds.contains(message.senderUserProfileId),
                                modifier =
                                    Modifier.animateItem(
                                        fadeInSpec = fadeAnimSpec,
                                        fadeOutSpec = fadeAnimSpec,
                                        placementSpec = placementAnimSpec,
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
            onClick = {
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

// ============================================================================================
// Previews
// ============================================================================================

@Preview(heightDp = 700)
@Composable
private fun ChatMessageList_ConversationPreview() {
    val messages = previewConversation()
    PreviewChatMessageList(messages = messages, readCount = messages.size)
}

@Preview(heightDp = 700)
@Composable
private fun ChatMessageList_ChatRulesWarnBoxPreview() {
    val messages = previewConversation()
    PreviewChatMessageList(
        messages = messages,
        readCount = messages.size,
        showChatRulesWarnBox = true,
    )
}

/**
 * The unread divider needs `canScrollDown`, so the list has to be long enough to actually scroll —
 * a four message conversation would render without the marker.
 */
@Preview(heightDp = 700)
@Composable
private fun ChatMessageList_UnreadMarkerPreview() {
    PreviewChatMessageList(messages = previewLongConversation(), readCount = 3)
}

/**
 * Both production call sites pass `Modifier.weight(1f)` from within a [Column]
 * (`TradeChatScreen`, `PrivateChatScreen`). The inner `LazyColumn` fills its parent, so without a
 * bounded height it collapses and the preview renders blank.
 */
@ExcludeFromCoverage
@Composable
private fun PreviewChatMessageList(
    messages: List<TwoPartyPrivateChatMessage>,
    readCount: Int,
    showChatRulesWarnBox: Boolean = false,
) {
    BisqTheme.Preview {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatMessageList(
                messages = messages,
                ignoredUserIds = emptySet(),
                showChatRulesWarnBox = showChatRulesWarnBox,
                readCount = readCount,
                userProfileIconProvider = { previewUserProfileIconProvider() },
                onResendMessage = {},
                userNameProvider = { it },
                onPeerProfileClick = {},
                modifier = Modifier.weight(1f),
                // The DM box, not the trade one: these previews feed the list TwoPartyPrivateChatMessages,
                // and TradePeerLeftMessageBox accepts PrivateChatMessage<*> so the wrong one compiles
                // and renders "has left the trade".
                leaveMessageContent = { message, modifier -> PrivateChatPeerLeftMessageBox(message, modifier) },
            )
        }
    }
}

@ExcludeFromCoverage
private fun previewUserProfileIconProvider(): suspend (UserProfileVO) -> PlatformImage = { createEmptyImage() }

/** `reverseLayout = true`, so index 0 renders at the bottom — newest message first. */
@ExcludeFromCoverage
private fun previewConversation() =
    listOf(
        previewMessage("msg4", "Alice", ChatMessageTypeEnum.LEAVE, text = null),
        previewMessage("msg3", "Alice", text = "Payment received, thanks!"),
        previewMessage("msg2", PREVIEW_MY_NAME, text = "Just sent it over."),
        previewMessage("msg1", "Alice", ChatMessageTypeEnum.PROTOCOL_LOG_MESSAGE, text = "Trade started"),
    )

@ExcludeFromCoverage
private fun previewLongConversation() =
    List(12) { i ->
        previewMessage(
            id = "msg$i",
            senderName = if (i % 2 == 0) "Alice" else PREVIEW_MY_NAME,
            text = "Message number ${12 - i}",
        )
    }

private const val PREVIEW_MY_NAME = "Bob"

/**
 * [id] must be unique per list — the `LazyColumn` keys on it. `isMyMessage` is derived from the
 * sender, so [senderName] of [PREVIEW_MY_NAME] yields an own message and anything else a peer one.
 */
@ExcludeFromCoverage
private fun previewMessage(
    id: String,
    senderName: String,
    chatMessageType: ChatMessageTypeEnum = ChatMessageTypeEnum.TEXT,
    text: String?,
) = createMockTwoPartyPrivateChatMessage(
    id = id,
    chatMessageType = chatMessageType,
    text = text,
    senderUserProfile = createMockUserProfile(senderName),
    myUserProfile = createMockUserProfile(PREVIEW_MY_NAME),
)
