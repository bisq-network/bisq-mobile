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
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessageModel
import network.bisq.mobile.domain.data.replicated.chat.reactions.BisqEasyOpenTradeMessageReactionVO
import network.bisq.mobile.domain.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TextMessageBox(
    message: BisqEasyOpenTradeMessageModel,
    userAvatar: PlatformImage? = null,
    onScrollToMessage: (String) -> Unit = {},
    onAddReaction: (ReactionEnum) -> Unit,
    onRemoveReaction: (BisqEasyOpenTradeMessageReactionVO) -> Unit,
    onReply: () -> Unit = {},
    onCopy: () -> Unit = {},
    onIgnoreUser: () -> Unit = {},
    onReportUser: () -> Unit = {}
) {
    val isMyMessage = message.isMyMessage
    val chatAlign = if (isMyMessage) Alignment.End else Alignment.Start
    val contentAlign = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
    val menuPosition = if (isMyMessage) Alignment.Start else Alignment.CenterHorizontally
    val bubbleBGColor = if (isMyMessage) BisqTheme.colors.primaryDisabled else BisqTheme.colors.dark_grey40
    val chatPadding =
        if (isMyMessage) PaddingValues(start = BisqUIConstants.ScreenPadding) else PaddingValues(end = BisqUIConstants.ScreenPadding)

    var showMenu by remember { mutableStateOf(false) }
    fun setShowMenu(value: Boolean) {
        showMenu = value
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = chatAlign,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)
    ) {
        UsernameAndDate(message)
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
                ProfileIconAndText(message, userAvatar)
            }
        }
        val messageBox = @Composable {
            Column(
                horizontalAlignment = chatAlign,
            ) {
                Surface(
                    color = bubbleBGColor,
                    shape = RoundedCornerShape(BisqUIConstants.ScreenPadding),
                    modifier = Modifier
                        .padding(chatPadding)
                        .wrapContentSize(contentAlign)
                        .combinedClickable(onLongClick = { showMenu = true }, onClick = {}),
                ) {
                    if (message.citation != null) {
                        quoteAndProfileIconAndText()
                    } else {
                        ProfileIconAndText(message, userAvatar)
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
            menuPosition = menuPosition,
            showMenu = showMenu,
            onSetShowMenu = { value -> setShowMenu(value) },
            onAddReaction = onAddReaction,
            onRemoveReaction = onRemoveReaction,
            onReply = onReply,
            onCopy = onCopy,
            onIgnoreUser = onIgnoreUser,
            onReportUser = onReportUser,
        )
    }
}