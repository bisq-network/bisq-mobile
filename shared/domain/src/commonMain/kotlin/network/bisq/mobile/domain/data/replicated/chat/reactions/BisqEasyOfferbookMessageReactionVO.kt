package network.bisq.mobile.domain.data.replicated.chat.reactions

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.chat.ChatChannelDomainEnum

@Serializable
data class BisqEasyOfferbookMessageReactionVO(
    val id: String,
    val userProfileId: String,
    val chatChannelId: String,
    val chatChannelDomain: ChatChannelDomainEnum,
    val chatMessageId: String,
    val reactionId: Int,
    val date: Long
)