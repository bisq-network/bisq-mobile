package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.createEmptyImage
import network.bisq.mobile.domain.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageDto
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatTextMessageBox(
    message: BisqEasyOpenTradeMessageModel,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onScrollToMessage: (String) -> Unit = {},
    onAddReaction: (ReactionEnum) -> Unit,
    onRemoveReaction: (BisqEasyOpenTradeMessageReactionVO) -> Unit,
    onReply: () -> Unit = {},
    onCopy: () -> Unit = {},
    onIgnoreUser: () -> Unit = {},
    onUndoIgnoreUser: () -> Unit = {},
    onReportUser: () -> Unit = {},
    isIgnored: Boolean,
    modifier: Modifier = Modifier,
    onResendMessage: (String) -> Unit,
    userNameProvider: suspend (String) -> String,
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
            messageDeliveryInfoByPeersProfileId = message.messageDeliveryStatus,
        )
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
                    }) {
                }
                ProfileIconAndText(message, userProfileIconProvider)
            }
        }
        val messageBox = @Composable {
            Column(
                horizontalAlignment = chatAlign,
            ) {
                Surface(
                    color = bubbleBGColor,
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                    modifier = Modifier
                        .padding(chatPadding)
                        .wrapContentSize(contentAlign)
                        .combinedClickable(onLongClick = { showMenu = true }, onClick = {}),
                ) {
                    if (message.citation != null) {
                        quoteAndProfileIconAndText()
                    } else {
                        ProfileIconAndText(message, userProfileIconProvider)
                    }
                }
            }
        }

        val reactions = @Composable {
            ReactionDisplay(
                message,
                onAddReaction = onAddReaction,
                onRemoveReaction = onRemoveReaction,
                modifier = Modifier.wrapContentSize(contentAlign)
            )
        }

        // If the message is short and there are less than 4 reactions,
        // show the message and reactions in a single row.
        val reactionsList by message.chatReactions.collectAsState()
        val groupedReactions = reactionsList.groupBy { it.reactionId }
        if (message.textString.length < 10 && groupedReactions.size < 4) {
            Row {
                if (message.isMyMessage) {
                    reactions()
                    messageBox()
                } else {
                    messageBox()
                    reactions()
                }
            }
        } else {
            Column(
                horizontalAlignment = chatAlign,
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)
            ) {
                messageBox()
                reactions()
            }
        }

        ChatMessageContextMenu(
            message = message,
            showMenu = showMenu,
            onSetShowMenu = { value -> setShowMenu(value) },
            onAddReaction = onAddReaction,
            onRemoveReaction = onRemoveReaction,
            onReply = onReply,
            onCopy = onCopy,
            onIgnoreUser = onIgnoreUser,
            onUndoIgnoreUser = onUndoIgnoreUser,
            onReportUser = onReportUser,
            isIgnored = isIgnored,
        )
    }
}

@Preview
@Composable
private fun ChatTextMessageBoxPreview_MyMessage() {
    BisqTheme.Preview {
        val myUserProfile = createMockUserProfile("Bob")
        val peerUserProfile = createMockUserProfile("Alice")

        val dto = BisqEasyOpenTradeMessageDto(
            tradeId = "trade123",
            messageId = "msg123",
            channelId = "channel123",
            senderUserProfile = myUserProfile,
            receiverUserProfileId = peerUserProfile.networkId.pubKey.id,
            receiverNetworkId = peerUserProfile.networkId,
            text = "Hello! I'm interested in this trade.",
            citation = null,
            date = 1234567890000L,
            mediator = null,
            chatMessageType = ChatMessageTypeEnum.TEXT,
            bisqEasyOffer = null,
            chatMessageReactions = emptySet(),
            citationAuthorUserProfile = null
        )

        val message = BisqEasyOpenTradeMessageModel(
            dto,
            myUserProfile,
            emptyList()
        )

        ChatTextMessageBox(
            message = message,
            userProfileIconProvider = { createEmptyImage() },
            onAddReaction = {},
            onRemoveReaction = {},
            isIgnored = false,
            onResendMessage = {},
            userNameProvider = { it },
        )
    }
}

@Preview
@Composable
private fun ChatTextMessageBoxPreview_PeerMessage() {
    BisqTheme.Preview {
        val myUserProfile = createMockUserProfile("Bob")
        val peerUserProfile = createMockUserProfile("Alice")

        val dto = BisqEasyOpenTradeMessageDto(
            tradeId = "trade123",
            messageId = "msg456",
            channelId = "channel123",
            senderUserProfile = peerUserProfile,
            receiverUserProfileId = myUserProfile.networkId.pubKey.id,
            receiverNetworkId = myUserProfile.networkId,
            text = "Sure! Let's proceed with the payment.",
            citation = null,
            date = 1234567890000L,
            mediator = null,
            chatMessageType = ChatMessageTypeEnum.TEXT,
            bisqEasyOffer = null,
            chatMessageReactions = emptySet(),
            citationAuthorUserProfile = null
        )

        val message = BisqEasyOpenTradeMessageModel(
            dto,
            myUserProfile,
            emptyList()
        )

        ChatTextMessageBox(
            message = message,
            userProfileIconProvider = { createEmptyImage() },
            onAddReaction = {},
            onRemoveReaction = {},
            isIgnored = false,
            onResendMessage = {},
            userNameProvider = { it }
        )
    }
}

@Preview
@Composable
private fun ChatTextMessageBoxPreview_LongMessage() {
    BisqTheme.Preview {
        val myUserProfile = createMockUserProfile("Bob")
        val peerUserProfile = createMockUserProfile("Alice")

        val dto = BisqEasyOpenTradeMessageDto(
            tradeId = "trade123",
            messageId = "msg789",
            channelId = "channel123",
            senderUserProfile = peerUserProfile,
            receiverUserProfileId = myUserProfile.networkId.pubKey.id,
            receiverNetworkId = myUserProfile.networkId,
            text = "This is a longer message to demonstrate how the chat message box handles multiple lines of text. It should wrap properly and maintain good readability with proper spacing and alignment.",
            citation = null,
            date = 1234567890000L,
            mediator = null,
            chatMessageType = ChatMessageTypeEnum.TEXT,
            bisqEasyOffer = null,
            chatMessageReactions = emptySet(),
            citationAuthorUserProfile = null
        )

        val message = BisqEasyOpenTradeMessageModel(
            dto,
            myUserProfile,
            emptyList(),
        )

        ChatTextMessageBox(
            message = message,
            userProfileIconProvider = { createEmptyImage() },
            onAddReaction = {},
            onRemoveReaction = {},
            isIgnored = false,
            onResendMessage = {},
            userNameProvider = { it }
        )
    }
}