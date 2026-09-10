package network.bisq.mobile.presentation.common.ui.components.molecules.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.ChatMessage
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.createMockBisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.chat.priv.messageDeliveryStatusOrNull
import network.bisq.mobile.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ChatMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <R : ChatMessageReaction> ChatTextMessageBox(
    isIgnored: Boolean,
    onAddReaction: (ReactionEnum) -> Unit,
    onRemoveReaction: (R) -> Unit,
    message: ChatMessage<R>,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onResendMessage: (String) -> Unit,
    userNameProvider: suspend (String) -> String,
    onPeerProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    onScrollToMessage: (String) -> Unit = {},
    onReply: () -> Unit = {},
    onCopy: () -> Unit = {},
    onIgnoreUser: () -> Unit = {},
    onUndoIgnoreUser: () -> Unit = {},
    onReportUser: () -> Unit = {},
    /** Null keeps the item out of the menu entirely; see [ChatMessageContextMenu]. */
    onEditMessage: (() -> Unit)? = null,
    onDeleteMessage: (() -> Unit)? = null,
    myProfiles: Collection<UserProfileVO> = emptyList(),
) {
    val isMyMessage = message.isMyMessage
    val chatAlign = if (isMyMessage) Alignment.End else Alignment.Start
    val contentAlign = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleBGColor =
        if (isMyMessage) BisqTheme.colors.primaryDisabled else BisqTheme.colors.dark_grey40
    val chatPadding =
        if (isMyMessage) PaddingValues(start = BisqUIConstants.ScreenPadding) else PaddingValues(end = BisqUIConstants.ScreenPadding)

    var showMenu by remember { mutableStateOf(false) }

    fun setShowMenu(value: Boolean) {
        showMenu = value
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = chatAlign,
        verticalArrangement = Arrangement.Center,
    ) {
        UsernameMessageDeliveryAndDate(
            message = message,
            onResendMessage = onResendMessage,
            userNameProvider = userNameProvider,
            messageDeliveryInfoByPeersProfileId = message.messageDeliveryStatusOrNull,
            onPeerProfileClick = onPeerProfileClick,
            onLongClick = { showMenu = true },
        )
        Spacer(modifier = Modifier.height(4.dp))
        val quoteAndProfileIconAndText = @Composable {
            Column(
                horizontalAlignment = chatAlign,
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
            ) {
                QuoteMessageBubble(
                    message,
                    onClick = {
                        val chatMessageId = message.citation?.chatMessageId
                        if (chatMessageId != null) {
                            onScrollToMessage(message.citation!!.chatMessageId!!)
                        }
                    },
                ) {
                }
                ProfileIconAndText(
                    message = message,
                    userProfileIconProvider = userProfileIconProvider,
                    onPeerProfileClick = onPeerProfileClick,
                    onLongClick = { showMenu = true },
                    myProfiles = myProfiles,
                )
            }
        }
        // Reactions always sit below the bubble. They used to move beside it for messages under ten
        // characters with fewer than four distinct reactions, which made a conversation change shape
        // from one message to the next for a reason the reader could not see.
        Column(
            horizontalAlignment = chatAlign,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            Surface(
                color = bubbleBGColor,
                shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                modifier =
                    Modifier
                        .padding(chatPadding)
                        .wrapContentSize(contentAlign)
                        .combinedClickable(onLongClick = { showMenu = true }, onClick = {}),
            ) {
                if (message.citation != null) {
                    quoteAndProfileIconAndText()
                } else {
                    ProfileIconAndText(
                        message = message,
                        userProfileIconProvider = userProfileIconProvider,
                        onPeerProfileClick = onPeerProfileClick,
                        onLongClick = { showMenu = true },
                        myProfiles = myProfiles,
                    )
                }
            }
            ReactionDisplay(
                message,
                onAddReaction = onAddReaction,
                onRemoveReaction = onRemoveReaction,
                modifier = Modifier.wrapContentSize(contentAlign),
            )
        }

        ChatMessageContextMenu(
            message = message,
            showMenu = showMenu,
            onSetShowMenu = { value -> setShowMenu(value) },
            onAddReaction = onAddReaction,
            onReply = onReply,
            onCopy = onCopy,
            onIgnoreUser = onIgnoreUser,
            onUndoIgnoreUser = onUndoIgnoreUser,
            onReportUser = onReportUser,
            isIgnored = isIgnored,
            onEditMessage = onEditMessage,
            onDeleteMessage = onDeleteMessage,
        )
    }
}

@Preview
@Composable
private fun ChatTextMessageBox_MyMessagePreview() {
    BisqTheme.Preview {
        val myUserProfile = createMockUserProfile("Bob")
        val peerUserProfile = createMockUserProfile("Alice")

        val message =
            createMockBisqEasyOpenTradeMessage(
                id = "msg123",
                text = "Hello! I'm interested in this trade.",
                senderUserProfile = myUserProfile,
                myUserProfile = myUserProfile,
                tradeId = "trade123",
            )

        ChatTextMessageBox(
            message = message,
            userProfileIconProvider = { createEmptyImage() },
            onAddReaction = {},
            onRemoveReaction = {},
            isIgnored = false,
            onResendMessage = {},
            userNameProvider = { it },
            onPeerProfileClick = {},
        )
    }
}

@Preview
@Composable
private fun ChatTextMessageBox_PeerMessagePreview() {
    BisqTheme.Preview {
        val myUserProfile = createMockUserProfile("Bob")
        val peerUserProfile = createMockUserProfile("Alice")

        val message =
            createMockBisqEasyOpenTradeMessage(
                id = "msg456",
                text = "Sure! Let's proceed with the payment.",
                senderUserProfile = peerUserProfile,
                myUserProfile = myUserProfile,
                tradeId = "trade123",
            )

        ChatTextMessageBox(
            message = message,
            userProfileIconProvider = { createEmptyImage() },
            onAddReaction = {},
            onRemoveReaction = {},
            isIgnored = false,
            onResendMessage = {},
            userNameProvider = { it },
            onPeerProfileClick = {},
        )
    }
}

/**
 * [ReactionDisplay] reads only the reaction id and its sender — it groups by the resolved
 * [ReactionEnum] and asks [ChatMessage.isMyChatReaction] who reacted. The rest is filled with
 * plausible values so the object stays a faithful one.
 */
@ExcludeFromCoverage
private fun previewReaction(
    id: String,
    sender: UserProfileVO,
    receiver: UserProfileVO,
    reaction: ReactionEnum,
) = BisqEasyOpenTradeMessageReaction(
    id = id,
    senderUserProfile = sender,
    receiverUserProfileId = receiver.id,
    receiverNetworkId = receiver.networkId,
    chatChannelId = "channel-1",
    chatChannelDomain = ChatChannelDomainEnum.BISQ_EASY_OPEN_TRADES,
    chatMessageId = "msg-1",
    reactionId = reaction.ordinal,
    date = 1234567890000L,
    isRemoved = false,
)

/**
 * Two thumbs-up — one of them mine, which is what makes a tap remove rather than add — so the count
 * badge shows, plus a single heart for the form without it.
 */
@ExcludeFromCoverage
private fun previewReactions(
    myUserProfile: UserProfileVO,
    peerUserProfile: UserProfileVO,
) = listOf(
    previewReaction("r-1", myUserProfile, peerUserProfile, ReactionEnum.THUMBS_UP),
    previewReaction("r-2", peerUserProfile, myUserProfile, ReactionEnum.THUMBS_UP),
    previewReaction("r-3", peerUserProfile, myUserProfile, ReactionEnum.HEART),
)

/**
 * The only preview that reaches [ReactionDisplay]: every other one passes no reactions, so the row
 * renders empty.
 *
 * The icons are grey placeholders in a preview: [network.bisq.mobile.presentation.common.ui
 * .components.atoms.DynamicImage] short-circuits under `LocalInspectionMode`. What this is good for
 * is the pill, the count and where the row sits relative to the bubble.
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun ChatTextMessageBox_ReactionsPreview() {
    BisqTheme.Preview {
        val myUserProfile = createMockUserProfile("Bob")
        val peerUserProfile = createMockUserProfile("Alice")

        val message =
            createMockBisqEasyOpenTradeMessage(
                id = "msg-1",
                text = "Payment sent, please confirm.",
                senderUserProfile = peerUserProfile,
                myUserProfile = myUserProfile,
                chatReactions = previewReactions(myUserProfile, peerUserProfile),
                tradeId = "trade123",
            )

        ChatTextMessageBox(
            message = message,
            userProfileIconProvider = { createEmptyImage() },
            onAddReaction = {},
            onRemoveReaction = {},
            isIgnored = false,
            onResendMessage = {},
            userNameProvider = { it },
            onPeerProfileClick = {},
        )
    }
}

/**
 * The only preview that reaches [QuoteMessageBubble]: it is drawn just for a message that carries a
 * citation. Alice replies quoting Bob, which is the way one usually meets a quote — someone
 * answering something you wrote.
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun ChatTextMessageBox_QuotedMessagePreview() {
    BisqTheme.Preview {
        val myUserProfile = createMockUserProfile("Bob")
        val peerUserProfile = createMockUserProfile("Alice")

        val message =
            createMockBisqEasyOpenTradeMessage(
                id = "msg-1",
                text = "Yes, that works for me.",
                citation =
                    Citation(
                        authorUserProfileId = "Bob",
                        text = "Can we settle tomorrow morning?",
                        chatMessageId = "msg-0",
                    ),
                citationAuthorUserProfile = myUserProfile,
                senderUserProfile = peerUserProfile,
                myUserProfile = myUserProfile,
                tradeId = "trade123",
            )

        ChatTextMessageBox(
            message = message,
            userProfileIconProvider = { createEmptyImage() },
            onAddReaction = {},
            onRemoveReaction = {},
            isIgnored = false,
            onResendMessage = {},
            userNameProvider = { it },
            onPeerProfileClick = {},
        )
    }
}

@Preview
@Composable
private fun ChatTextMessageBox_LongMessagePreview() {
    BisqTheme.Preview {
        val myUserProfile = createMockUserProfile("Bob")
        val peerUserProfile = createMockUserProfile("Alice")

        val message =
            createMockBisqEasyOpenTradeMessage(
                id = "msg789",
                text = "This is a longer message to demonstrate how the chat message box handles multiple lines of text. It should wrap properly and maintain good readability with proper spacing and alignment.",
                senderUserProfile = peerUserProfile,
                myUserProfile = myUserProfile,
                tradeId = "trade123",
            )

        ChatTextMessageBox(
            message = message,
            userProfileIconProvider = { createEmptyImage() },
            onAddReaction = {},
            onRemoveReaction = {},
            isIgnored = false,
            onResendMessage = {},
            userNameProvider = { it },
            onPeerProfileClick = {},
        )
    }
}

@Preview
@Composable
@ExcludeFromCoverage
private fun ChatTextMessageBox_MentionHighlightPreview() {
    BisqTheme.Preview {
        val myUserProfile = createMockUserProfile("Bob")
        val peerUserProfile = createMockUserProfile("Alice")

        val message =
            createMockBisqEasyOpenTradeMessage(
                id = "msg-mention",
                text = "hey @Bob can you confirm?",
                senderUserProfile = peerUserProfile,
                myUserProfile = myUserProfile,
                tradeId = "trade123",
            )

        ChatTextMessageBox(
            message = message,
            userProfileIconProvider = { createEmptyImage() },
            onAddReaction = {},
            onRemoveReaction = {},
            isIgnored = false,
            onResendMessage = {},
            userNameProvider = { it },
            onPeerProfileClick = {},
            myProfiles = listOf(myUserProfile),
        )
    }
}
