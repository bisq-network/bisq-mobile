package network.bisq.mobile.presentation.ui.composeModels

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource

data class BottomNavigationItem(val title: String, val route: String, val icon: DrawableResource)
data class PagerViewItem(val title: String, val image: DrawableResource, val desc: String)

@Immutable
data class ChatMessage(
    val messageID: String,
    val author: String,
    val content: String,
    val timestamp: String,
    var reaction: String, // TODO: List of ReactionType
    val chatMessageReplyOf: ChatMessageReplyOf?
)

data class ChatMessageReplyOf(
    val messageID: String,
    val author: String,
    val content: String
)