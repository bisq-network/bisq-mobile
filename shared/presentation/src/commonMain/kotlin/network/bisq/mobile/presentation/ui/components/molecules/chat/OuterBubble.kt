package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.composeModels.ChatMessage
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

enum class ReactionType(val fileName: String) {
    THUMBS_UP("drawable/icon_reaction_thumbs_up.png"),
    THUMBS_DOWN("drawable/icon_reaction_thumbs_down.png"),
    HEART("drawable/icon_reaction_heart.png"),
    LAUGH("drawable/icon_reaction_laugh.png"),
    HAPPY("drawable/icon_reaction_happy.png"),
    PARTY("drawable/icon_reaction_party.png")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatOuterBubble(
    message: ChatMessage,
    isUserMe: Boolean,
    onQuoteMessage: (ChatMessage) -> Unit = {}
) {

    if (message.author == "SYSTEM") {
        ChatSystemMessage(message)
        return
    }

    val chatAlign = if (isUserMe) {
        Alignment.End
    } else {
        Alignment.Start
    }

    val contentAlign = if (isUserMe) {
        Alignment.CenterEnd
    } else {
        Alignment.CenterStart
    }

    val menuPosition = if (isUserMe) {
        Alignment.Start
    } else {
        Alignment.CenterHorizontally
    }

    val chatReactionAlignment = if (isUserMe) {
        Alignment.BottomStart
    } else {
        Alignment.BottomEnd
    }

    val bubbleBGColor = if (isUserMe) {
        BisqTheme.colors.primaryDisabled
    } else {
        BisqTheme.colors.secondaryDisabled
    }

    var showMenu by remember { mutableStateOf(false) }
    fun setShowMenu(value: Boolean) {
        showMenu = value
    }

    val chatPadding = if (isUserMe) {
        PaddingValues(start = 32.dp)
    } else {
        PaddingValues(end = 32.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = chatAlign,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)
    ) {
        ChatAuthorNameTimestamp(message)
        Box {
            Surface(
                color = bubbleBGColor,
                shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf),
                modifier = Modifier.padding(chatPadding).wrapContentSize(contentAlign),
            ) {
                Box(
                    modifier = Modifier.combinedClickable(onLongClick = { showMenu = true }, onClick = {}),
                ) {
                    if (message.chatMessageReplyOf?.content?.isNotEmpty() == true) {
                        QuoteMessageBubble(message) {
                            ChatInnerBubble(message, isUserMe)
                        }
                    } else {
                        ChatInnerBubble(message, isUserMe)
                    }
                }
            }
            if (message.reaction.isNotEmpty()) {
                ChatReactionShow(
                    reaction = message.reaction,
                    modifier = Modifier
                        .align(chatReactionAlignment)
                        .offset(y = 5.dp)
                        .padding(chatPadding)
                )
            }
        }
        ChatPopup(
            message = message,
            menuPosition = menuPosition,
            showMenu = showMenu,
            onSetShowMenu = { value -> setShowMenu(value) },
            onQuoteMessage = onQuoteMessage,
        )

    }
}